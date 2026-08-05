import { useState } from "react";
import {
  repositories,
} from "../../data/repositories";
import type { TabId } from "../../types";
import {
  ModuleAccessGuard,
} from "../access";
import {
  LavaClientsManager,
} from "./clients";
import {
  LavaHistoryPanel,
} from "./history";
import {
  LavaOrderForm,
} from "./orders";
import {
  LavaReports,
} from "./reports/LavaReports";
import {
  LavaServicesManager,
} from "./services";
import {
  LavaDomainProvider,
  useLavaDomain,
} from "./state";
import {
  ContentStack,
  SubSectionTabs,
} from "../../components/ui";

const initialLavaDomainState =
  {
    services: [],
    clients: [],
    progressVehicles: [],
    financeEntries: [],
    historyEntries: [],
  };

export function LavaContent({
  activeTab,
  adminModeActive,
}: {
  activeTab: TabId;
  adminModeActive: boolean;
}) {
  return (
    <ModuleAccessGuard moduleId="lava">
      <LavaDomainProvider
        initialState={initialLavaDomainState}
        repository={repositories.lava}
      >
        <LavaContentView
          activeTab={activeTab}
          adminModeActive={adminModeActive}
        />
      </LavaDomainProvider>
    </ModuleAccessGuard>
  );
}

function LavaContentView({
  activeTab,
  adminModeActive,
}: {
  activeTab: TabId;
  adminModeActive: boolean;
}) {
  const [lavaServiceView, setLavaServiceView] = useState<"order" | "catalog">("order");
  const {
    state: domainState,
    createService,
    updateService,
    setServiceActive,
    removeService,
    createClient,
    updateClient,
    setClientActive,
    removeClient,
    openWorkOrder,
    payWorkOrder,
    changeWorkOrderStage,
    listHistory,
  } = useLavaDomain();

  const {
    services,
    clients,
    historyEntries,
  } = domainState;

  const effectiveLavaServiceView =
    !adminModeActive &&
    lavaServiceView === "catalog"
      ? "order"
      : lavaServiceView;

  if (activeTab === "lava-service") {
    return (
      <ContentStack>
        {adminModeActive ? (
          <SubSectionTabs
            active={effectiveLavaServiceView}
            onChange={(id) =>
              setLavaServiceView(
                id === "catalog"
                  ? "catalog"
                  : "order"
              )
            }
            options={[
              {
                id: "order",
                label: "Abertura OS",
              },
              {
                id: "catalog",
                label: "Serviços",
              },
            ]}
          />
        ) : null}

        {(!adminModeActive ||
          effectiveLavaServiceView === "order") && (
          <LavaOrderForm
            clients={clients}
            services={services}
            onCreateOrder={async (order) => {
              const created =
                await openWorkOrder(order);

              if (!created) {
                return null;
              }

              return created;
            }}
            onPayOrder={payWorkOrder}
            onCompleteOrder={changeWorkOrderStage}
          />
        )}

        {adminModeActive &&
          effectiveLavaServiceView === "catalog" && (
            <LavaServicesManager
              services={services}
              onCreate={createService}
              onUpdate={updateService}
              onSetActive={setServiceActive}
              onRemove={removeService}
            />
          )}

      </ContentStack>
    );
  }

  if (activeTab === "lava-history") {
    return (
      <ContentStack>
        <LavaHistoryPanel
          entries={historyEntries}
          onLoadHistory={listHistory}
        />
      </ContentStack>
    );
  }

  if (activeTab === "lava-clients") {
    return (
      <LavaClientsManager
        clients={clients}
        onCreate={createClient}
        onUpdate={updateClient}
        onSetActive={setClientActive}
        onRemove={removeClient}
      />
    );
  }

  if (activeTab === "lava-reports") {
    return <LavaReports />;
  }

  return null;
}
