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

    val categorizationTool: Tool =
        Tool.newBuilder()
            .addFunctionDeclarations(
                FunctionDeclaration.newBuilder()
                    .setName("categorize_transaction")
                    .setDescription("Categorize an M-PESA transaction based on the counterparty name and context.")
                    .setParameters(
                        Schema.newBuilder()
                            .setType(OBJECT)
                            .putProperties("transaction_id",
                                Schema.newBuilder().setType(STRING).setDescription("The transaction ID to categorize.").build())
                            .putProperties("category",
                                Schema.newBuilder().setType(STRING).setFormat("enum")
                                    .addEnum("groceries")           // Supermarkets, food stores
                                    .addEnum("transport")           // Matatu, taxi, fuel
                                    .addEnum("bills")              // Utilities, rent, phone
                                    .addEnum("entertainment")       // Movies, dining, leisure
                                    .addEnum("shopping")           // Clothes, electronics
                                    .addEnum("health")             // Hospital, pharmacy
                                    .addEnum("education")          // School fees, books
                                    .addEnum("business")           // Business payments
                                    .addEnum("personal")           // Friends, family transfers
                                    .addEnum("savings")            // Bank transfers, investments
                                    .addEnum("other")              // Unknown/uncategorized
                                    .setDescription("The transaction category based on counterparty and context.").build())
                            .putProperties("confidence",
                                Schema.newBuilder().setType(STRING).setFormat("enum")
                                    .addEnum("high")    // Very confident in categorization
                                    .addEnum("medium")  // Somewhat confident
                                    .addEnum("low")     // Best guess
                                    .setDescription("Confidence level in the categorization.").build())
                            .addRequired("transaction_id")
                            .addRequired("category")
                            .addRequired("confidence")
                            .build()
                    )
            )
            .build()

    // Combined tools for chained function calling
    val allTools: List<Tool> = listOf(mpesaSmsTool, categorizationTool)
} 