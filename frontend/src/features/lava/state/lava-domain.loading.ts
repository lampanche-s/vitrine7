import type {
  LavaDomainState,
} from "./lava-domain.types";

export function hasLavaDomainData(
  state: LavaDomainState
): boolean {
  return (
    state.services.length > 0 ||
    state.clients.length > 0 ||
    state.progressVehicles.length > 0 ||
    state.financeEntries.length > 0 ||
    state.historyEntries.length > 0
  );
}

export function shouldShowLavaInitialLoading(
  hasLoadedSnapshot: boolean,
  state: LavaDomainState
): boolean {
  return (
    !hasLoadedSnapshot &&
    !hasLavaDomainData(state)
  );
}
