import type {
  BarDomainAction,
  BarDomainState,
} from "./bar-domain.types";

export function barDomainReducer(
  state: BarDomainState,
  action: BarDomainAction
): BarDomainState {
  switch (action.type) {
    case "repository/snapshot-loaded":
      return action.payload;

    case "history/replaced":
      return {
        ...state,
        historyEntries: action.payload,
      };

    case "command/created":
      return {
        ...state,
        commands: [
          action.payload,
          ...state.commands,
        ],
      };

    case "command/updated":
      return {
        ...state,
        commands: state.commands.map(
          (command) =>
            command.id === action.payload.id
              ? action.payload
              : command
        ),
      };

    case "command/removed":
      return {
        ...state,
        commands: state.commands.filter(
          (command) =>
            command.id !== action.payload
        ),
      };

    case "command/closed":
      return {
        ...state,
        commands: state.commands.filter(
          (command) =>
            command.id !== action.payload.commandId
        ),
        historyEntries: [
          action.payload.historyEntry,
          ...state.historyEntries,
        ],
        catalogEntries:
          action.payload.catalogEntries ??
          state.catalogEntries,
      };

    case "catalog/created":
      return {
        ...state,
        catalogEntries: [
          action.payload,
          ...state.catalogEntries,
        ],
      };

    case "catalog/updated":
      return {
        ...state,
        catalogEntries: state.catalogEntries.map(
          (entry) =>
            entry.id === action.payload.id
              ? action.payload
              : entry
        ),
      };

    case "catalog/removed":
      return {
        ...state,
        catalogEntries: state.catalogEntries.filter(
          (entry) => entry.id !== action.payload
        ),
      };

    default:
      return state;
  }
}
