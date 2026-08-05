import {
  useEffect,
  useState,
} from "react";

import {
  ModuleAccessGuard,
} from "../access";

import {
  ContentStack,
  useToast,
} from "../../components/ui";

import type {
  Client,
  ClientInput,
} from "../../entities/client";

import {
  repositories,
} from "../../data/repositories";

import {
  ClientsManager,
} from "./ClientsManager";

export function ClientsContent() {
  return (
    <ModuleAccessGuard moduleId="clients">
      <ClientsContentView />
    </ModuleAccessGuard>
  );
}

function ClientsContentView() {
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const { showToast } = useToast();

  useEffect(() => {
    let current = true;

    repositories.clients.list()
      .then((loadedClients) => {
        if (current) {
          setClients(loadedClients);
        }
      })
      .catch((error: unknown) => {
        if (current) {
          showToast({
            title: error instanceof Error
              ? error.message
              : "Não foi possível carregar os clientes.",
            variant: "error",
            dedupeKey: "clients-load-failed",
          });
        }
      })
      .finally(() => {
        if (current) {
          setLoading(false);
        }
      });

    return () => {
      current = false;
    };
  }, [showToast]);

  async function create(input: ClientInput) {
    try {
      const client = await repositories.clients.create(input);
      setClients((current) => [client, ...current]);
      return true;
    } catch (error) {
      showToast({
        title: error instanceof Error
          ? error.message
          : "Não foi possível cadastrar o cliente.",
        variant: "error",
      });
      return false;
    }
  }

  async function update(clientId: number, input: ClientInput) {
    try {
      const client = await repositories.clients.update(clientId, input);
      setClients((current) => current.map((item) =>
        item.id === client.id ? client : item
      ));
      return true;
    } catch (error) {
      showToast({
        title: error instanceof Error
          ? error.message
          : "Não foi possível atualizar o cliente.",
        variant: "error",
      });
      return false;
    }
  }

  async function setActive(clientId: number, active: boolean) {
    try {
      const client = await repositories.clients.setActive(clientId, active);
      setClients((current) => current.map((item) =>
        item.id === client.id ? client : item
      ));
      return true;
    } catch (error) {
      showToast({
        title: error instanceof Error
          ? error.message
          : "Não foi possível alterar o status do cliente.",
        variant: "error",
      });
      return false;
    }
  }

  async function remove(clientId: number) {
    try {
      await repositories.clients.remove(clientId);
      setClients((current) => current.filter((item) => item.id !== clientId));
      return true;
    } catch (error) {
      showToast({
        title: error instanceof Error
          ? error.message
          : "Não foi possível excluir o cliente.",
        variant: "error",
      });
      return false;
    }
  }

  return (
    <ContentStack>
      {loading ? (
        <p className="text-sm text-[var(--text-subtle)]">
          Carregando clientes...
        </p>
      ) : (
        <ClientsManager
          clients={clients}
          onCreate={create}
          onUpdate={update}
          onSetActive={setActive}
          onRemove={remove}
        />
      )}
    </ContentStack>
  );
}
