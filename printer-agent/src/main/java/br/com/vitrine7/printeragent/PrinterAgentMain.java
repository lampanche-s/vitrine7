package br.com.vitrine7.printeragent;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class PrinterAgentMain {
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private PrinterAgentMain() {
    }

    public static void main(String[] args) {
        try {
            Path configPath = args.length > 0
                    ? Path.of(args[0])
                    : Path.of("printer-agent.properties");

            AgentConfig config = AgentConfig.load(configPath, System.getenv());
            PrintJobClient client = new PrintJobClient(config);
            ThermalPrinter printer = new ThermalPrinter(config);

            log("Agente iniciado.");
            log("Backend: " + config.backendUrl());
            log("Impressora: " + config.printerName());

            boolean backendValidated = false;

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    var delivery = client.next();
                    if (!backendValidated) {
                        log("Comunicacao com o backend validada.");
                        backendValidated = true;
                    }
                    if (delivery.isEmpty()) {
                        continue;
                    }

                    PrintJobClient.Delivery job = delivery.get();
                    log("Imprimindo trabalho " + job.id() + " (tentativa " + job.attempt() + ").");

                    try {
                        printer.print(job.receiptText());
                        client.report(job.id(), true, null);
                        log("Trabalho " + job.id() + " concluido.");
                    } catch (Exception printError) {
                        String message = shortMessage(printError);
                        log("Falha no trabalho " + job.id() + ": " + message);
                        try {
                            client.report(job.id(), false, message);
                        } catch (Exception reportError) {
                            log("Nao foi possivel informar a falha ao servidor: " + shortMessage(reportError));
                        }
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (Exception exception) {
                    log("Falha de comunicacao: " + shortMessage(exception));
                    try {
                        Thread.sleep(3000L);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } catch (Exception exception) {
            System.err.println("Nao foi possivel iniciar o agente: " + shortMessage(exception));
            System.exit(1);
        }
    }

    private static void log(String message) {
        System.out.println("[" + LOG_TIME.format(LocalDateTime.now()) + "] " + message);
    }

    private static String shortMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 450 ? normalized : normalized.substring(0, 450);
    }
}
