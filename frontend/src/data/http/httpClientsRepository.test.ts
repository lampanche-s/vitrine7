import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";

const mocks = vi.hoisted(() => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

vi.mock("../../shared/http", () => ({
  httpClient: mocks.httpClient,
}));

import {
  httpClientsRepository,
} from "./httpClientsRepository";

const response = {
  id: 7,
  name: "Ana Silva",
  phone: "71999990000",
  vehicleName: "Honda Civic",
  plate: "ABC1D23",
  active: true,
};

describe("httpClientsRepository", () => {
  beforeEach(() => {
    Object.values(mocks.httpClient).forEach((mock) => mock.mockReset());
  });

  it("carrega e mapeia clientes da API canônica", async () => {
    mocks.httpClient.get.mockResolvedValue({
      items: [response],
      page: 0,
      totalPages: 1,
    });

    await expect(httpClientsRepository.list()).resolves.toEqual([
      {
        id: 7,
        name: "Ana Silva",
        phone: "(71) 99999-0000",
        vehicle: "Honda Civic",
        plate: "ABC1D23",
        active: true,
      },
    ]);

    expect(mocks.httpClient.get).toHaveBeenCalledWith(
      "/clients?page=0&size=100&sort=name&direction=ASC"
    );
  });

  it("usa os endpoints canônicos de criação, edição, status e exclusão", async () => {
    mocks.httpClient.post.mockResolvedValue(response);
    mocks.httpClient.put.mockResolvedValue(response);
    mocks.httpClient.patch.mockResolvedValue({
      ...response,
      active: false,
    });
    mocks.httpClient.delete.mockResolvedValue(undefined);

    const input = {
      name: "Ana Silva",
      phone: "(71) 99999-0000",
      vehicle: "Honda Civic",
      plate: "ABC1D23",
    };

    await httpClientsRepository.create(input);
    await httpClientsRepository.update(7, input);
    await httpClientsRepository.setActive(7, false);
    await httpClientsRepository.remove(7);

    const body = {
      name: "Ana Silva",
      phone: "(71) 99999-0000",
      vehicleName: "Honda Civic",
      plate: "ABC1D23",
    };

    expect(mocks.httpClient.post).toHaveBeenCalledWith("/clients", body);
    expect(mocks.httpClient.put).toHaveBeenCalledWith("/clients/7", body);
    expect(mocks.httpClient.patch).toHaveBeenCalledWith(
      "/clients/7/active",
      { active: false }
    );
    expect(mocks.httpClient.delete).toHaveBeenCalledWith("/clients/7");
  });
});
