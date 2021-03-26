/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.parser.vtb;

import lombok.Getter;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static ru.investbook.parser.vtb.VtbDerivativeTransactionTable.VtbDerivativeTransactionTableHeader.*;

public class VtbDerivativeTransactionTable extends AbstractReportTable<DerivativeTransaction> {

    private static final String TABLE_NAME = "Сделки с Производными финансовыми инструментами в отчетном периоде";

    protected VtbDerivativeTransactionTable(BrokerReport report) {
        super(report, TABLE_NAME, null, VtbDerivativeTransactionTableHeader.class);
    }

    @Override
    protected Collection<DerivativeTransaction> getRow(TableRow row) {

        boolean isBuy = row.getStringCellValue(DIRECTION).equalsIgnoreCase("покупка");
        int count = row.getIntCellValue(COUNT);
        BigDecimal valueInPoints = row.getBigDecimalCellValue(QUOTE).multiply(BigDecimal.valueOf(count));
        if (isBuy) {
            valueInPoints = valueInPoints.negate();
        }
        BigDecimal commission = row.getBigDecimalCellValue(BROKER_CLEARING_COMMISSION)
                .add(row.getBigDecimalCellValue(BROKER_TRANSACTION_COMMISSION))
                .negate();
        return Collections.singleton(DerivativeTransaction.builder()
                .timestamp(row.getInstantCellValue(DATE_TIME))
                .transactionId(row.getStringCellValue(TRANSACTION))
                .portfolio(getReport().getPortfolio())
                .security(row.getStringCellValue(CONTRACT))
                .count((isBuy ? 1 : -1) * count)
                .valueInPoints(valueInPoints)
                .commission(commission)
                .commissionCurrency("RUB") // FORTS, only RUB
                .build());
    }

    enum VtbDerivativeTransactionTableHeader implements TableColumnDescription {
        DATE_TIME("Дата и время заключения сделки"),
        TRANSACTION("№ сделки"),
        CONTRACT("Фьючерсный контракт", "опцион, код"),
        DIRECTION("Вид сделки"),
        COUNT("Количество"),
        QUOTE("Цена контракта", "размер премии", "пункты"),
//        VALUE("сумма срочной сделки"), // не предоставляется в отчете ВТБ
        BROKER_CLEARING_COMMISSION("Комиссия Банка за расчет по сделке"),
        BROKER_TRANSACTION_COMMISSION("Комиссия Банка за заключение сделки");

        @Getter
        private final TableColumn column;

        VtbDerivativeTransactionTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
