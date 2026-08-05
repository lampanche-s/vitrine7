import {
  describe,
  expect,
  it,
} from "vitest";

import {
  createInitialBarDomainState,
} from "../../../data/test-fixtures/bar.fixture";

import {
  barDomainReducer,
} from "./bar-domain.reducer";

describe("barDomainReducer", () => {
  it("substitui o estado pelo snapshot do repository", () => {
    const nextState = barDomainReducer(
      createInitialBarDomainState(),
      {
        type: "repository/snapshot-loaded",
        payload: {
          commands: [],
          catalogEntries: [],
          historyEntries: [],
        },
      }
    );

    expect(nextState).toEqual({
      commands: [],
      catalogEntries: [],
      historyEntries: [],
    });
  });

  it("atualiza uma entrada do catálogo canônico", () => {
    const state = createInitialBarDomainState();
    const updatedEntry = {
      ...state.catalogEntries[0]!,
      price: 20,
    };

    const nextState = barDomainReducer(
      state,
      {
        type: "catalog/updated",
        payload: updatedEntry,
      }
    );

    expect(nextState.catalogEntries[0]).toEqual(
      updatedEntry
    );
  });
});
