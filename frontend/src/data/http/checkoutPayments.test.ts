import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";

const httpClient = {
  post: vi.fn(),
};

vi.mock("../../shared/http", () => ({
  httpClient,
}));

function createMemoryStorage(): Storage {
  const values =
    new Map<string, string>();

  return {
    get length() {
      return values.size;
    },

    clear() {
      values.clear();
    },

    getItem(key) {
      return values.get(key) ?? null;
    },

    key(index) {
      return (
        Array.from(values.keys())[index] ??
        null
      );
    },

    removeItem(key) {
      values.delete(key);
    },

    setItem(key, value) {
      values.set(key, value);
    },
  };
}

function idempotencyKeyFromCall(
  callIndex: number
): string {
  const options =
    httpClient.post.mock.calls[
      callIndex
    ]?.[2] as
      | {
          headers?: Record<
            string,
            string
          >;
        }
      | undefined;

  return (
    options?.headers?.[
      "Idempotency-Key"
    ] ?? ""
  );
}

describe(
  "checkout payment idempotency",
  () => {
    beforeEach(() => {
      vi.resetModules();
      vi.clearAllMocks();

      vi.stubGlobal("window", {
        sessionStorage:
          createMemoryStorage(),
      });
    });

    it(
      "reuses the key after an ambiguous failure",
      async () => {
        httpClient.post
          .mockRejectedValueOnce(
            new Error(
              "Connection failed"
            )
          )
          .mockResolvedValueOnce({
            checkout: {
              id: "checkout-1",
              status: "FINALIZED",
              totalCents: 2000,
              finalizedAt: null,
            },
            payment: {
              status: "APPROVED",
            },
          });

        const {
          runCheckoutPayment,
        } = await import(
          "./checkoutPayments"
        );

        const input = {
          checkoutId: "checkout-1",
          processing: "pix" as const,
          method: "PIX" as const,
        };

        await expect(
          runCheckoutPayment(input)
        ).rejects.toThrow(
          "Connection failed"
        );

        await runCheckoutPayment(input);

        expect(
          idempotencyKeyFromCall(0)
        ).not.toBe("");

        expect(
          idempotencyKeyFromCall(1)
        ).toBe(
          idempotencyKeyFromCall(0)
        );
      }
    );

    it(
      "creates a new key after a definitive decline",
      async () => {
        httpClient.post
          .mockResolvedValueOnce({
            checkout: {
              id: "checkout-2",
              status:
                "PAYMENT_FAILED",
              totalCents: 2000,
              finalizedAt: null,
            },
            payment: {
              status: "DECLINED",
            },
          })
          .mockResolvedValueOnce({
            checkout: {
              id: "checkout-2",
              status: "FINALIZED",
              totalCents: 2000,
              finalizedAt: null,
            },
            payment: {
              status: "APPROVED",
            },
          });

        const {
          runCheckoutPayment,
        } = await import(
          "./checkoutPayments"
        );

        const input = {
          checkoutId: "checkout-2",
          processing: "pix" as const,
          method: "PIX" as const,
        };

        await runCheckoutPayment(input);
        await runCheckoutPayment(input);

        expect(
          idempotencyKeyFromCall(1)
        ).not.toBe(
          idempotencyKeyFromCall(0)
        );
      }
    );

    it(
      "keeps the key in sessionStorage to survive refresh",
      async () => {
        const storage =
          createMemoryStorage();

        vi.stubGlobal("window", {
          sessionStorage: storage,
        });

        httpClient.post
          .mockRejectedValue(
            new Error("Timeout")
          );

        const firstModule =
          await import(
            "./checkoutPayments"
          );

        await expect(
          firstModule.runCheckoutPayment({
            checkoutId:
              "checkout-3",
            processing: "cash",
            method: "CASH",
            cashReceivedCents: 5000,
          })
        ).rejects.toThrow("Timeout");

        const firstKey =
          idempotencyKeyFromCall(0);

        vi.resetModules();

        const reloadedModule =
          await import(
            "./checkoutPayments"
          );

        await expect(
          reloadedModule.runCheckoutPayment({
            checkoutId:
              "checkout-3",
            processing: "cash",
            method: "CASH",
            cashReceivedCents: 5000,
          })
        ).rejects.toThrow("Timeout");

        expect(
          idempotencyKeyFromCall(1)
        ).toBe(firstKey);
      }
    );
  }
);
