package br.com.vitrine7.pagbankagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record PaymentTerminalCapabilities(
        boolean credit,
        boolean debit,
        boolean pix,
        boolean simulated,
        String driver
) {
    public PaymentTerminalCapabilities {
        pix = false;
    }

    public ObjectNode toJson(ObjectMapper mapper) {
        ObjectNode capabilities = mapper.createObjectNode();
        capabilities.put("credit", credit);
        capabilities.put("debit", debit);
        capabilities.put("pix", pix);
        capabilities.put("simulated", simulated);
        capabilities.put("driver", driver);

        ArrayNode paymentMethods = mapper.createArrayNode();
        if (credit) {
            paymentMethods.add("CREDIT_CARD");
        }
        if (debit) {
            paymentMethods.add("DEBIT_CARD");
        }
        capabilities.set("paymentMethods", paymentMethods);
        return capabilities;
    }
}
