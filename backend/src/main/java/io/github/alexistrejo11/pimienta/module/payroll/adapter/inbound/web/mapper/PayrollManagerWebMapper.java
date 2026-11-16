package io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.request.RegisterPayrollAdjustmentRequest;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.request.RegisterPayrollPaymentRequest;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.request.RegisterPayrollPeriodRequest;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.request.RegisterPayrollRecordRequest;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.response.PayrollDebtResponse;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.response.PayrollPaymentResponse;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.response.PayrollPeriodResponse;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.response.PayrollRecordResponse;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.response.PayrollSummaryResponse;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.command.RegisterPayrollAdjustmentCommand;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.command.RegisterPayrollPaymentCommand;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.command.RegisterPayrollPeriodCommand;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.command.RegisterPayrollRecordCommand;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollDebt;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollPayment;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollPeriod;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollRecord;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollSummary;
import java.time.ZoneOffset;

public final class PayrollManagerWebMapper {

  private PayrollManagerWebMapper() {}

  public static RegisterPayrollPaymentCommand toCommand(RegisterPayrollPaymentRequest request) {
    return new RegisterPayrollPaymentCommand(
        request.employeeId(),
        request.frequency(),
        request.workedDaysStart(),
        request.workedDaysEnd(),
        request.grossAmount(),
        request.netAmount(),
        request.destinationAccount(),
        request.transactionId());
  }

  public static RegisterPayrollAdjustmentCommand toCommand(
      Long recordId, RegisterPayrollAdjustmentRequest request) {
    return new RegisterPayrollAdjustmentCommand(recordId, request.type(), request.amount(), request.reason());
  }

  public static RegisterPayrollPeriodCommand toCommand(RegisterPayrollPeriodRequest request) {
    return new RegisterPayrollPeriodCommand(request.frequency(), request.startDate(), request.endDate());
  }

  public static RegisterPayrollRecordCommand toCommand(RegisterPayrollRecordRequest request) {
    return new RegisterPayrollRecordCommand(
        request.employeeId(),
        request.periodId(),
        request.workedDaysStart(),
        request.workedDaysEnd(),
        request.grossAmount());
  }

  public static PayrollPaymentResponse toPaymentResponse(PayrollPayment payment) {
    return new PayrollPaymentResponse(
        payment.getId(),
        payment.getEmployeeId(),
        payment.getFrequency(),
        payment.getWorkedDaysStart(),
        payment.getWorkedDaysEnd(),
        payment.getGrossAmount(),
        payment.getNetAmount(),
        payment.getDestinationAccount(),
        payment.getTransactionId(),
        payment.getStatus(),
        payment.getPendingAmount(),
        payment.getCreatedAt() != null ? payment.getCreatedAt().toInstant(ZoneOffset.UTC) : null);
  }

  public static PayrollSummaryResponse toSummaryResponse(PayrollSummary summary) {
    return new PayrollSummaryResponse(
        summary.getFrom(),
        summary.getTo(),
        summary.getEmployeesPaid(),
        summary.getTotalGross(),
        summary.getTotalNet(),
        summary.getTotalDiscounts(),
        summary.getTotalBonuses(),
        summary.getTotalDebt());
  }

  public static PayrollDebtResponse toDebtResponse(PayrollDebt debt) {
    return new PayrollDebtResponse(
        debt.getId(),
        debt.getEmployeeId(),
        debt.getPayrollRecordId(),
        debt.getAmountOwed(),
        debt.getReason(),
        debt.isSettled(),
        debt.getCreatedAt() != null ? debt.getCreatedAt().toInstant(ZoneOffset.UTC) : null,
        debt.getSettledAt() != null ? debt.getSettledAt().toInstant(ZoneOffset.UTC) : null);
  }

  public static PayrollPeriodResponse toPeriodResponse(PayrollPeriod period) {
    return new PayrollPeriodResponse(
        period.getId(),
        period.getFrequency(),
        period.getStartDate(),
        period.getEndDate(),
        period.getStatus());
  }

  public static PayrollRecordResponse toRecordResponse(PayrollRecord record) {
    return new PayrollRecordResponse(
        record.getId(),
        record.getEmployeeId(),
        record.getPeriodId(),
        record.getWorkedDaysStart(),
        record.getWorkedDaysEnd(),
        record.getGrossAmount(),
        record.getTotalDiscounts(),
        record.getTotalBonuses(),
        record.getNetAmount(),
        record.getStatus());
  }
}
