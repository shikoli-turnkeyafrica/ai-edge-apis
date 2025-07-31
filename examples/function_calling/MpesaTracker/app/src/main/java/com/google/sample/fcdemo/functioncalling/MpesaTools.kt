package com.google.sample.fcdemo.functioncalling

import com.google.ai.edge.localagents.core.proto.FunctionDeclaration
import com.google.ai.edge.localagents.core.proto.Schema
import com.google.ai.edge.localagents.core.proto.Tool
import com.google.ai.edge.localagents.core.proto.Type.OBJECT
import com.google.ai.edge.localagents.core.proto.Type.STRING

object MpesaTools {
    val mpesaSmsTool: Tool =
        Tool.newBuilder()
            .addFunctionDeclarations(
                FunctionDeclaration.newBuilder()
                    .setName("parse_mpesa_sms")
                    .setDescription("Parse an M-PESA transaction SMS and return structured data.")
                    .setParameters(
                        Schema.newBuilder()
                            .setType(OBJECT)
                            .putProperties("transaction_id",
                                Schema.newBuilder().setType(STRING).setDescription("The unique alphanumeric transaction code, e.g., 'TG34DDA8UK'.").build())
                            .putProperties("direction",
                                Schema.newBuilder().setType(STRING).setFormat("enum")
                                    .addEnum("sent")
                                    .addEnum("paid")
                                    .addEnum("received")
                                    .setDescription("The direction of the transaction, such as 'sent' or 'received'.").build())
                            .putProperties("amount_kes",
                                Schema.newBuilder().setType(STRING).setDescription("The transaction amount in Kenyan Shillings, e.g., '20.00'.").build())
                            .putProperties("counterparty",
                                Schema.newBuilder().setType(STRING).setDescription("The name of the person or business the transaction was with, e.g., 'Linda Makatiani'.").build())
                            .putProperties("date_time",
                                Schema.newBuilder().setType(STRING).setDescription("The date and time of the transaction, e.g., '3/7/25 at 1:17 AM'.").build())
                            .addRequired("transaction_id")
                            .addRequired("direction")
                            .addRequired("amount_kes")
                            .addRequired("counterparty")
                            .addRequired("date_time")
                            .build()
                    )
            )
            .build()
} 