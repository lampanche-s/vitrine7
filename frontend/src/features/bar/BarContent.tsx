import {
  repositories,
} from "../../data/repositories";

import type {
  TabId,
} from "../../types";

import type {
  BarDomainState,
} from "./state";

import {
  ContentStack,
} from "../../components/ui";

import {
  ModuleAccessGuard,
} from "../access";

import {
  BarDomainProvider,
  useBarDomain,
} from "./state";

import {
  BarCommandsManager,
} from "./commands";

import {
  BarMenuManager,
} from "./catalog";

import {
  BarSalesHistory,
} from "./history";

import {
  BarReports,
} from "./reports/BarReports";

function createBarDomainInitialState(): BarDomainState {
  return {
    commands: [],
    catalogEntries: [],
    historyEntries: [],
  };
}

export function BarContent({
  activeTab,
}: {
  activeTab: TabId;
}) {
  return (
    <ModuleAccessGuard moduleId="bar">
      <BarDomainProvider
        initialState={createBarDomainInitialState()}
        repository={repositories.bar}
      >
        <BarContentView activeTab={activeTab} />
      </BarDomainProvider>
    </ModuleAccessGuard>
  );
}

function BarContentView({
  activeTab,
}: {
  activeTab: TabId;
}) {
  const {
    state: domainState,
    listHistory,

    openCommand,
    reopenCommand,
    setCommandStatus,
    printPrePaymentNote,
    printItems,
    printServices,
    printLine,
    addCommandItem,
    updateCommandItemQuantity,
    removeCommandItem,
    cancelCommand,
    closeCommand,
    closeVoucher,

    createCatalogEntry,
    updateCatalogEntry,
    removeCatalogEntry,
  } = useBarDomain();

  const {
    commands,
    catalogEntries,
  } = domainState;

  if (activeTab === "bar-order") {
    return (
      <ContentStack stretch>
        <BarCommandsManager
          commands={commands}
          catalogEntries={catalogEntries}
          onOpenCommand={openCommand}
          onSetCommandStatus={setCommandStatus}
          onPrintPrePaymentNote={printPrePaymentNote}
          onPrintItems={printItems}
          onPrintServices={printServices}
          onPrintLine={printLine}
          onAddCommandItem={addCommandItem}
          onUpdateCommandItemQuantity={
            updateCommandItemQuantity
          }
          onRemoveCommandItem={
            removeCommandItem
          }
          onCancelCommand={
            cancelCommand
          }
          onCloseCommand={
            closeCommand
          }
          onCloseVoucher={closeVoucher}
        />
      </ContentStack>
    );
  }

  if (activeTab === "bar-menu") {
    return (
      <ContentStack>
        <BarMenuManager
          catalogEntries={catalogEntries}
          onCreate={createCatalogEntry}
          onUpdate={updateCatalogEntry}
          onRemove={removeCatalogEntry}
        />
      </ContentStack>
    );
  }

  if (activeTab === "bar-finance") {
    return (
      <ContentStack>
        <BarSalesHistory
          onLoadHistory={listHistory}
          onReopenCommand={reopenCommand}
        />
      </ContentStack>
    );
  }

  if (activeTab === "bar-reports") {
    return (
      <ModuleAccessGuard moduleId="reports">
        <ContentStack>
          <BarReports
            repository={repositories.reports}
          />
        </ContentStack>
      </ModuleAccessGuard>
    );
  }

  return null;
}
