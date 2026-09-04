import type {
  AdminDomainAction,
  AdminDomainState,
} from "./admin-domain.types";

export function adminDomainReducer(
  state: AdminDomainState,
  action: AdminDomainAction
): AdminDomainState {
  switch (action.type) {
    case "repository/snapshot-loaded":
      return action.payload;

    case "user/created":
      return {
        ...state,
        users: [
          action.payload,
          ...state.users,
        ],
      };

    case "user/updated":
      return {
        ...state,
        users: state.users.map((user) =>
          user.id === action.payload.id
            ? action.payload
            : user
        ),
      };

    case "user/removed":
      return {
        ...state,
        users: state.users.filter(
          (user) =>
            user.id !== action.payload
        ),
      };

    default:
      return state;
  }
}
