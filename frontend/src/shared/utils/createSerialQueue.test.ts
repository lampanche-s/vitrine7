import { describe, expect, it } from "vitest";
import { createSerialQueue } from "./createSerialQueue";

describe("createSerialQueue", () => {
  it("processa nove adições rápidas em ordem, uma por vez", async () => {
    const enqueue = createSerialQueue();
    const completed: number[] = [];
    let releaseFirst: () => void = () => undefined;
    const firstGate = new Promise<void>((resolve) => {
      releaseFirst = resolve;
    });
    let active = 0;
    let maximumActive = 0;

    const requests = Array.from({ length: 9 }, (_, index) =>
      enqueue(async () => {
        active += 1;
        maximumActive = Math.max(maximumActive, active);
        if (index === 0) await firstGate;
        completed.push(index + 1);
        active -= 1;
      })
    );

    releaseFirst();
    await Promise.all(requests);

    expect(completed).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9]);
    expect(maximumActive).toBe(1);
  });
});
