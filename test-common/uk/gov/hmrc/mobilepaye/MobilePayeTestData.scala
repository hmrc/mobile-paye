/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.mobilepaye

import java.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.domain.audit.{MobilePayeSummaryResponseAudit, OtherIncomeAudit, PayeIncomeAudit}
import uk.gov.hmrc.mobilepaye.domain.citizendetails.Person
import uk.gov.hmrc.mobilepaye.domain.simpleassessment.ReasonType.UNDERPAYMENT
import uk.gov.hmrc.mobilepaye.domain.simpleassessment.{MobileSAReconciliation, MobileSATaxYearReconciliation, MobileSimpleAssessmentResponse, Reason, Receipts}
import uk.gov.hmrc.mobilepaye.domain.{HistoricTaxCodeIncome, IncomeSource, IncomeTaxYear, MobilePayePreviousYearSummaryResponse, MobilePayeSummaryResponse, OtherIncome, P800Repayment, PayeIncome, TaxCodeChange}
import uk.gov.hmrc.mobilepaye.domain.tai.*
import uk.gov.hmrc.mobilepaye.domain.taxcalc.P800Status.{NotSupported, Underpaid}
import uk.gov.hmrc.mobilepaye.domain.taxcalc.RepaymentStatus.{ChequeSent, PaymentDue, PaymentPaid, SaUser}
import uk.gov.hmrc.mobilepaye.domain.taxcalc.{P800Status, P800Summary, RepaymentStatus, TaxYearReconciliation}
import uk.gov.hmrc.time.TaxYear

import scala.collection.immutable.Seq

trait MobilePayeTestData {
  val currentTaxYear: Int = TaxYear.current.startYear
  val endOfTaxYear: Int = TaxYear.current.finishYear
  val previousTaxYear: Int = currentTaxYear - 1
  val cyMinus2: Int = currentTaxYear - 2
  val cyMinus3: Int = currentTaxYear - 3

  val nino: Nino = Nino("CS700100A")

  def taxCodeIncomeNew1(employmentType: TaxComponentType = EmploymentIncome,
                        status: TaxCodeIncomeStatus = Live,
                        empId: Int = 3,
                        name: String,
                        amount: BigDecimal = 1000,
                        taxCode: String
                       ): TaxCodeIncome =
    TaxCodeIncome(componentType = employmentType, status = status, employmentId = Some(empId), name = name, amount = amount, taxCode = taxCode)
  val taxCodeIncome: TaxCodeIncome = TaxCodeIncome(componentType = EmploymentIncome,
                                                   status        = Live,
                                                   employmentId  = Some(3),
                                                   name          = "The Best Shop Ltd",
                                                   amount        = 1000,
                                                   taxCode       = "S1150L"
                                                  )
  val taxCodeIncomeNew1 = taxCodeIncome.copy(name = "TESCO")

  val taxCodeIncome2: TaxCodeIncome = taxCodeIncome.copy(name = "The Worst Shop Ltd", employmentId = Some(4))
  val taxCodeIncomeNew2 = taxCodeIncome2.copy(name = "ASDA")

  val taxCodeIncome3: TaxCodeIncome =
    taxCodeIncome.copy(componentType = PensionIncome, name = "Prestige Pensions", employmentId = Some(5))

  val otherNonTaxCodeIncome: OtherNonTaxCodeIncome = OtherNonTaxCodeIncome(StatePension, BigDecimal(250.0))
  val untaxedIncome: Option[UntaxedInterest] = Some(UntaxedInterest(UntaxedInterestIncome, BigDecimal(250.0)))
  val nonTaxCodeIncome: NonTaxCodeIncome = NonTaxCodeIncome(None, Seq(otherNonTaxCodeIncome))

  val nonTaxCodeIncomeWithUntaxedInterest: NonTaxCodeIncome =
    NonTaxCodeIncome(untaxedIncome, Seq(otherNonTaxCodeIncome))
  val nonTaxCodeIncomeWithoutUntaxedInterest: NonTaxCodeIncome = NonTaxCodeIncome(None, Seq(otherNonTaxCodeIncome))

  val payments: Seq[Payment] = Seq(
    Payment(LocalDate.now(), 100, 20, 10, 50, 5, 2),
    Payment(LocalDate.now().minusDays(10), 80, 20, 10, 20, 5, 2),
    Payment(LocalDate.now().minusDays(20), 50, 20, 10, 30, 5, 2)
  )

  val payments2: Seq[Payment] = Seq(
    Payment(LocalDate.now().plusDays(1), 100, 20, 10, 50, 5, 2),
    Payment(LocalDate.now().minusDays(10), 80, 20, 10, 20, 5, 2),
    Payment(LocalDate.now().minusDays(20), 50, 20, 10, 30, 5, 2)
  )

  def annualAccount(taxYear: Int = LocalDate.now().getYear, rtiStatus: RealTimeStatus = Available, seqNo: Int): AnnualAccount =
    AnnualAccount(seqNo, TaxYear(taxYear), payments, rtiStatus)
  val annualAccount2: AnnualAccount = AnnualAccount(1, taxYear = TaxYear(startYear = LocalDate.now().getYear), payments2, realTimeStatus = Available)

  def taiEmployment(taxYear: Int = LocalDate.now().getYear, rtiStatus: RealTimeStatus = Available, seqNo: Int = 3): Employment =
    Employment(
      name              = "TESCO",
      employmentStatus  = Live,
      payrollNumber     = Some("ABC123"),
      sequenceNumber    = seqNo,
      payeNumber        = "P12345",
      startDate         = Some(LocalDate.now().minusYears(4)),
      endDate           = None,
      annualAccounts    = Seq(annualAccount(taxYear, rtiStatus, seqNo)),
      taxDistrictNumber = "123",
      employmentType    = EmploymentIncome
    )

  val annualAccountsNew1 =
    annualAccount(LocalDate.now().getYear, Available, 3)

  val annualAccountsNew2 =
    AnnualAccount(4,
                  TaxYear(startYear = TaxYear.current.back(4).currentYear),
                  Seq(Payment(LocalDate.now().minusDays(63), 50, 20, 10, 30, 5, 2)),
                  Available
                 )

  val annualAccountsNew3 =
    AnnualAccount(4, TaxYear(TaxYear.current.back(5).currentYear), Seq(Payment(LocalDate.now().minusDays(63), 50, 20, 10, 30, 5, 2)), Available)
  val annualAccountsNew5 =
    annualAccount(LocalDate.now().getYear, Available, 5)
  val annualAccounts2 = Seq(
    AnnualAccount(4,
                  TaxYear(startYear = TaxYear.current.back(4).currentYear),
                  Seq(Payment(LocalDate.now().minusDays(63), 50, 20, 10, 30, 5, 2)),
                  Available
                 ),
    AnnualAccount(4, TaxYear(TaxYear.current.back(5).currentYear), Seq(Payment(LocalDate.now().minusDays(63), 50, 20, 10, 30, 5, 2)), Available)
  )
  val taiEmployment2: Employment = Employment(
    name              = "ASDA",
    employmentStatus  = Live,
    payrollNumber     = Some("DEF456"),
    sequenceNumber    = 4,
    payeNumber        = "P54321",
    startDate         = Some(TaxYear.current.back(5).starts),
    endDate           = Some(TaxYear.current.back(3).starts),
    annualAccounts    = annualAccounts2,
    taxDistrictNumber = "123",
    employmentType    = EmploymentIncome
  )

  val taiEmploymentNew4: Employment = Employment(
    name              = "HMRC",
    employmentStatus  = Ceased,
    payrollNumber     = Some("HM456"),
    sequenceNumber    = 6,
    payeNumber        = "P5HM234",
    startDate         = Some(TaxYear.current.back(5).starts),
    endDate           = Some(TaxYear.current.back(3).starts),
    annualAccounts    = annualAccounts2,
    taxDistrictNumber = "123456",
    employmentType    = EmploymentIncome
  )
  val taiEmploymentNew5: Employment = Employment(
    name              = "Sainsbury",
    employmentStatus  = NotLive,
    payrollNumber     = Some("SN456"),
    sequenceNumber    = 7,
    payeNumber        = "SNHM234",
    startDate         = Some(TaxYear.current.back(5).starts),
    endDate           = Some(TaxYear.current.back(3).starts),
    annualAccounts    = Seq.empty,
    taxDistrictNumber = "SN123456",
    employmentType    = EmploymentIncome
  )

  val taxCodeIncome4: TaxCodeIncome =
    TaxCodeIncome(componentType = EmploymentIncome, status = Ceased, employmentId = Some(6), name = "HMRC", amount = 1000, taxCode = "S1150L")

  val taxCodeIncome5: TaxCodeIncome =
    TaxCodeIncome(componentType = EmploymentIncome, status = NotLive, employmentId = Some(7), name = "Sainsbury", amount = 1000, taxCode = "S1150L")

  val incomeSourceCeased: IncomeSource = IncomeSource(taxCodeIncome4, taiEmploymentNew4)

  val taiEmploymentOnly = taiEmployment2.copy(annualAccounts = Seq.empty)

  val annualAccountsRtiSeq = Seq(
    AnnualAccount(1, TaxYear(TaxYear.current.currentYear), Seq(Payment(LocalDate.now().minusDays(63), 50, 20, 10, 30, 5, 2)), Available),
    AnnualAccount(2, TaxYear(TaxYear.current.currentYear), Seq(Payment(LocalDate.now().minusDays(63), 50, 20, 10, 30, 5, 2)), Available)
  )

  val taiEmploymentLatest: Employment = Employment(
    name             = "ASDA",
    employmentStatus = Live,
    payrollNumber    = Some("DEF456"),
    sequenceNumber   = 4,
    payeNumber       = "P54321",
    startDate        = Some(TaxYear.current.back(5).starts),
    endDate          = Some(TaxYear.current.back(3).starts),
    annualAccounts = Seq(
      AnnualAccount(1, TaxYear(TaxYear.current.currentYear), Seq(Payment(LocalDate.now().minusDays(63), 50, 20, 10, 30, 5, 2)), Available),
      AnnualAccount(2, TaxYear(TaxYear.current.currentYear), Seq(Payment(LocalDate.now().minusDays(63), 50, 20, 10, 30, 5, 2)), Available)
    ),
    taxDistrictNumber = "123",
    employmentType    = EmploymentIncome
  )

  val taiEmployment3: Employment =
    taiEmployment().copy(payrollNumber = None, sequenceNumber = 5)

  val taiEmploymentNew3: Employment =
    taiEmployment().copy(payrollNumber = None, sequenceNumber = 5, name = "Prestige Pensions", annualAccounts = Seq(annualAccountsNew5))

  val taiEmployment4: Employment =
    taiEmployment3.copy(annualAccounts = Seq(annualAccount2))

  val taiEmployment5: Employment = taiEmployment2.copy(
    annualAccounts = Seq(AnnualAccount(1, TaxYear(LocalDate.now().getYear), Seq.empty, Available))
  )

  val noBenefits: Benefits = Benefits(Seq.empty, Seq.empty)

  val otherBenefits: Seq[GenericBenefit] = Seq(
    GenericBenefit(MedicalInsurance, Some(3), BigDecimal(650)),
    GenericBenefit(MedicalInsurance, Some(4), BigDecimal(350)),
    GenericBenefit(Telephone, Some(4), BigDecimal(100)),
    GenericBenefit(Expenses, Some(3), BigDecimal(200)),
    GenericBenefit(Mileage, Some(3), BigDecimal(250))
  )

  val companyCarBenefits: Seq[CompanyCarBenefit] =
    Seq(CompanyCarBenefit(3, BigDecimal(20000), Seq.empty), CompanyCarBenefit(5, BigDecimal(15000), Seq.empty))
  val allBenefits: Benefits = Benefits(companyCarBenefits, otherBenefits)

  val employmentIncomeSource: Seq[IncomeSource] =
    Seq(IncomeSource(taxCodeIncome, taiEmployment()), IncomeSource(taxCodeIncome2, taiEmployment2))

  val employmentIncomeSourceNew = Seq(IncomeSource(taxCodeIncomeNew1, taiEmployment()), IncomeSource(taxCodeIncomeNew2, taiEmployment2))
  val previousEmpIncomeSource =
    Seq(IncomeSource(taxCodeIncome4, taiEmploymentNew4.copy(annualAccounts = Seq.empty)), IncomeSource(taxCodeIncome5, taiEmploymentNew5))

  val previousEmpIncomeSource4 = Seq(IncomeSource(taxCodeIncome5, taiEmploymentNew5))
  val previousEmpIncomeSourceNew =
    Seq(IncomeSource(taxCodeIncome4, taiEmploymentNew4.copy(annualAccounts = Seq.empty)), IncomeSource(taxCodeIncome5, taiEmploymentNew5))

  val employmentIncomeSourceWithRtiUnavail: Seq[IncomeSource] =
    Seq(IncomeSource(taxCodeIncome, taiEmployment(rtiStatus = TemporarilyUnavailable)), IncomeSource(taxCodeIncome2, taiEmployment2))

  val employmentIncomeSource2: Seq[IncomeSource] =
    Seq(IncomeSource(taxCodeIncome, taiEmployment4))

  val employmentIncomeSourceNoPayments: Seq[IncomeSource] =
    Seq(IncomeSource(taxCodeIncome, taiEmployment5))

  val employmentIncomeSourceWelsh: Seq[IncomeSource] =
    Seq(IncomeSource(taxCodeIncome.copy(taxCode = "C1150L"), taiEmployment()), IncomeSource(taxCodeIncome2, taiEmployment2))

  val employmentIncomeSourceUK: Seq[IncomeSource] =
    Seq(IncomeSource(taxCodeIncome.copy(taxCode = "1150L"), taiEmployment()), IncomeSource(taxCodeIncome2, taiEmployment2))
  val pensionIncomeSource: Seq[IncomeSource] = Seq(IncomeSource(taxCodeIncome3, taiEmployment3))
  val pensionIncomeSourceNew: Seq[IncomeSource] = Seq(
    IncomeSource(taxCodeIncome3, taiEmployment3.copy(name = "Prestige Pensions", employmentType = PensionIncome, sequenceNumber = 3))
  )

  val pensionIncomeSourceNoPension: Seq[IncomeSource] = Seq.empty

  val employments: Seq[PayeIncome] =
    employmentIncomeSource.map(ic => PayeIncome.fromIncomeSource(ic, employment = true))

  val employmentsNew = employmentIncomeSourceNew.map(ic => PayeIncome.fromIncomeSource(ic, employment = true))
  val previosEmploymentNew = previousEmpIncomeSource.map(ic => PayeIncome.fromIncomeSource(ic, employment = true))

  val welshEmployments: Seq[PayeIncome] =
    employmentIncomeSourceWelsh.map(ic => PayeIncome.fromIncomeSource(ic, employment = true))

  val ukEmployments: Seq[PayeIncome] =
    employmentIncomeSourceUK.map(ic => PayeIncome.fromIncomeSource(ic, employment = true))

  val pensions: Seq[PayeIncome] =
    pensionIncomeSource.map(ic => PayeIncome.fromIncomeSource(ic, employment = false))

  val pensionsNew: Seq[PayeIncome] =
    pensionIncomeSourceNew.map(ic => PayeIncome.fromIncomeSource(ic, employment = false))

  val taxAccountSummary: TaxAccountSummary = TaxAccountSummary(BigDecimal(250), BigDecimal(10000))
  val person: Person = Person(nino, "Carrot", "Smith")
  val otherIncome: OtherIncome = OtherIncome("STATE PENSION", 250.0, None)

  def repayment(
    p800Status: P800Status,
    paymentStatus: RepaymentStatus,
    taxYear: Int,
    amount: BigDecimal,
    time: LocalDate
  ): Option[P800Repayment] = {
    def withPaidDate(): Option[LocalDate] =
      paymentStatus match {
        case PaymentPaid | ChequeSent => Option(LocalDate.from(time))
        case _                        => None
      }

    val summary = P800Summary(p800Status, Some(paymentStatus), Some(amount), withPaidDate())

    P800Summary.toP800Repayment(summary, taxYear)
  }

  val otherIncomes: Seq[OtherIncome] = Seq(otherIncome)

  val fullMobileSimpleAssessmentResponse: MobileSimpleAssessmentResponse = MobileSimpleAssessmentResponse(
    List(
      MobileSATaxYearReconciliation(
        taxYear = previousTaxYear,
        reconciliations = List(
          MobileSAReconciliation(
            reconciliationId         = 1,
            reconciliationStatus     = Some(5),
            cumulativeAmount         = 200,
            taxLiabilityAmount       = 300,
            taxPaidAmount            = 100,
            reconciliationTimeStamp  = Some(s"$previousTaxYear-07-30 12:34:56"),
            p800Status               = Some("ISSUED"),
            collectionMethod         = None,
            previousReconciliationId = Some(2),
            nextReconciliationId     = None,
            multiYearRecIndicator    = None,
            p800Reasons = Some(
              List(
                Reason(reasonType = UNDERPAYMENT, reasonCode = 45, estimatedAmount = Some(175), actualAmount = Some(185))
              )
            ),
            businessReason   = "P302",
            eligibility      = true,
            totalAmountOwed  = 400,
            chargeReference  = Some("XQ004100001540"),
            dueDate          = Some(s"${currentTaxYear + 1}-01-31"),
            dunningLock      = None,
            receivableStatus = "OUTSTANDING",
            receipts = Some(
              List(
                Receipts(
                  receiptAmount      = Some(100.00),
                  receiptDate        = Some(s"$currentTaxYear-08-02"),
                  receiptMethod      = Some("RECEIVED FROM ETMP"),
                  receiptStatus      = Some("ALLOCATED"),
                  taxYearCodedOut    = None,
                  allocatedAmount    = Some(100.00),
                  promiseToPayRef    = None,
                  receiptDescription = Some("PAID-ONLINE")
                )
              )
            )
          )
        ),
        cardPaymentFallbackUrl = s"/tax-you-paid/$previousTaxYear-$currentTaxYear/paid-too-little"
      ),
      MobileSATaxYearReconciliation(
        taxYear = previousTaxYear - 1,
        reconciliations = List(
          MobileSAReconciliation(
            reconciliationId         = 2,
            reconciliationStatus     = Some(5),
            cumulativeAmount         = 100,
            taxLiabilityAmount       = 200,
            taxPaidAmount            = 50,
            reconciliationTimeStamp  = Some(s"${previousTaxYear - 1}-07-30 12:34:56"),
            p800Status               = Some("ISSUED"),
            collectionMethod         = None,
            previousReconciliationId = None,
            nextReconciliationId     = None,
            multiYearRecIndicator    = None,
            p800Reasons = Some(
              List(
                Reason(reasonType = UNDERPAYMENT, reasonCode = 45, estimatedAmount = Some(175), actualAmount = Some(185))
              )
            ),
            businessReason   = "P302",
            eligibility      = true,
            totalAmountOwed  = 300,
            chargeReference  = Some("XQ004100001539"),
            dueDate          = Some(s"${currentTaxYear + 1}-01-31"),
            dunningLock      = None,
            receivableStatus = "OUTSTANDING",
            receipts = Some(
              List(
                Receipts(
                  receiptAmount   = Some(150.00),
                  receiptDate     = Some(s"$previousTaxYear-08-02"),
                  receiptMethod   = Some("RECEIVED FROM ETMP"),
                  receiptStatus   = Some("ALLOCATED"),
                  taxYearCodedOut = None,
                  allocatedAmount = Some(150.00),
                  promiseToPayRef = None
                )
              )
            )
          )
        ),
        cardPaymentFallbackUrl = s"/tax-you-paid/${previousTaxYear - 1}-$previousTaxYear/paid-too-little"
      )
    )
  )

  val taxCalcTaxYearReconciliationResponse: List[TaxYearReconciliation] = {
    List(
      TaxYearReconciliation(
        taxYear        = previousTaxYear,
        reconciliation = P800Summary(_type = Underpaid, status = Some(PaymentDue), amount = Some(200), datePaid = None)
      ),
      TaxYearReconciliation(
        taxYear        = previousTaxYear - 1,
        reconciliation = P800Summary(_type = NotSupported, status = Some(SaUser), amount = None, datePaid = None)
      )
    )
  }

  val fullMobilePayeResponse: MobilePayeSummaryResponse = MobilePayeSummaryResponse(
    taxYear                = Some(TaxYear.current.currentYear),
    employments            = Some(employments),
    previousEmployments    = None,
    repayment              = None,
    pensions               = Some(pensions),
    otherIncomes           = Some(otherIncomes),
    simpleAssessment       = None,
    taxCodeChange          = Some(TaxCodeChange(hasChanged = true, Some(LocalDate.now().minusYears(2)))),
    taxFreeAmount          = Some(10000),
    estimatedTaxAmount     = Some(250),
    previousTaxYearLink    = s"/check-income-tax/income-tax-history",
    currentYearPlusOneLink = None
  )

  val fullMobilePayeResponseWithCY1Link: MobilePayeSummaryResponse = MobilePayeSummaryResponse(
    taxYear             = Some(TaxYear.current.currentYear),
    employments         = Some(employments),
    previousEmployments = None,
    repayment           = None,
    pensions            = Some(pensions),
    otherIncomes        = Some(otherIncomes),
    simpleAssessment    = None,
    taxCodeChange       = Some(TaxCodeChange(hasChanged = true, Some(LocalDate.now().minusYears(2)))),
    taxFreeAmount       = Some(10000),
    estimatedTaxAmount  = Some(250),
    previousTaxYearLink = s"/check-income-tax/income-tax-history",
    taxCodeLocation     = Some("Scottish")
  )

  val fullMobilePayeAudit: MobilePayeSummaryResponseAudit = MobilePayeSummaryResponseAudit(
    taxYear             = Some(TaxYear.current.currentYear),
    employments         = Some(employments.map(employment => PayeIncomeAudit.fromPayeIncome(employment.copy(payrollNumber = None)))),
    previousEmployments = None,
    repayment           = None,
    pensions            = Some(pensions.map(pension => PayeIncomeAudit.fromPayeIncome(pension.copy(payrollNumber = None)))),
    otherIncomes        = Some(otherIncomes.map(otherIncs => OtherIncomeAudit.fromOtherIncome(otherIncs))),
    taxCodeChange       = Some(TaxCodeChange(hasChanged = true, Some(LocalDate.now().minusYears(2)))),
    simpleAssessment    = None,
    taxFreeAmount       = Some(10000),
    estimatedTaxAmount  = Some(250)
  )

  val emptyIncomeTaxHistoryList: List[IncomeTaxYear] = {
    List(
      IncomeTaxYear(TaxYear.current, None),
      IncomeTaxYear(TaxYear.current.back(1), None),
      IncomeTaxYear(TaxYear.current.back(2), None),
      IncomeTaxYear(TaxYear.current.back(3), None),
      IncomeTaxYear(TaxYear.current.back(4), None),
      IncomeTaxYear(TaxYear.current.back(5), None)
    )
  }

  val historicTaxCodeIncome1: HistoricTaxCodeIncome = HistoricTaxCodeIncome(
    name          = taiEmployment().name,
    payrollNumber = taiEmployment().payrollNumber.get,
    payeReference = s"${taiEmployment().taxDistrictNumber}/${taiEmployment().payeNumber}",
    startDate     = taiEmployment().startDate,
    endDate       = None,
    amount        = taiEmployment().annualAccounts.head.payments.lastOption.map(_.amountYearToDate),
    taxAmount     = taiEmployment().annualAccounts.head.payments.lastOption.map(_.taxAmountYearToDate),
    taxCode       = Some(taxCodeIncome.taxCode)
  )

  val historicTaxCodeIncome2: HistoricTaxCodeIncome = HistoricTaxCodeIncome(
    name          = taiEmployment2.name,
    payrollNumber = taiEmployment2.payrollNumber.get,
    payeReference = s"${taiEmployment2.taxDistrictNumber}/${taiEmployment2.payeNumber}",
    startDate     = taiEmployment2.startDate,
    endDate       = taiEmployment2.endDate,
    amount        = None,
    taxAmount     = None,
    taxCode       = Some(taxCodeIncome.taxCode)
  )

  val historicTaxCodeIncome3: HistoricTaxCodeIncome = HistoricTaxCodeIncome(
    name          = taiEmployment3.name,
    payrollNumber = s"${taiEmployment3.taxDistrictNumber}/${taiEmployment3.payeNumber}",
    payeReference = s"${taiEmployment3.taxDistrictNumber}/${taiEmployment3.payeNumber}",
    startDate     = taiEmployment3.startDate,
    endDate       = None,
    amount        = None,
    taxAmount     = None,
    taxCode       = Some(taxCodeIncome.taxCode),
    isPension     = true
  )

  val fullIncomeTaxHistoryList: List[IncomeTaxYear] = {
    List(
      IncomeTaxYear(TaxYear.current, Some(Seq(historicTaxCodeIncome1))),
      IncomeTaxYear(TaxYear.current.back(1), Some(Seq(historicTaxCodeIncome1))),
      IncomeTaxYear(TaxYear.current.back(2), Some(Seq(historicTaxCodeIncome1, historicTaxCodeIncome2))),
      IncomeTaxYear(TaxYear.current.back(3), Some(Seq(historicTaxCodeIncome2))),
      IncomeTaxYear(
        TaxYear.current.back(4),
        Some(Seq(historicTaxCodeIncome3, historicTaxCodeIncome2.copy(amount = Some(50), taxAmount = Some(20))))
      ),
      IncomeTaxYear(TaxYear.current.back(5), None)
    )
  }

  val fullIncomeTaxHistoryListParsingError: List[IncomeTaxYear] = {
    List(
      IncomeTaxYear(TaxYear.current, None),
      IncomeTaxYear(TaxYear.current.back(1), Some(Seq(historicTaxCodeIncome1))),
      IncomeTaxYear(TaxYear.current.back(2), None),
      IncomeTaxYear(TaxYear.current.back(3), None),
      IncomeTaxYear(TaxYear.current.back(4), None),
      IncomeTaxYear(TaxYear.current.back(5), None)
    )
  }

  def fullMobilePayePreviousYearResponse(taxYear: Int = previousTaxYear): MobilePayePreviousYearSummaryResponse =
    MobilePayePreviousYearSummaryResponse(
      taxYear = Some(taxYear),
      employments = Some(
        Seq(PayeIncome.fromEmployment(taiEmployment(TaxYear.current.previous.startYear), Some("1250L"), None, taxYear))
      ),
      previousEmployments = Some(
        Seq(PayeIncome.fromEmployment(taiEmployment2.copy(employmentStatus = Ceased), Some("1199L"), None, taxYear))
      ),
      pensions = Some(
        Seq(
          PayeIncome.fromEmployment(taiEmployment3.copy(name = "ALDI", employmentType = PensionIncome), None, None, taxYear)
        )
      ),
      otherIncomes           = Some(otherIncomes),
      taxFreeAmount          = Some(10000),
      estimatedTaxAmount     = Some(250),
      payeSomethingWrongLink = s"/check-income-tax/update-income-details/decision/$taxYear"
    )

  def fullMobilePayePYAudit(taxYear: Int = previousTaxYear): MobilePayeSummaryResponseAudit =
    MobilePayeSummaryResponseAudit(
      taxYear = Some(taxYear),
      employments = Some(
        Seq(
          PayeIncomeAudit
            .fromPayeIncome(
              PayeIncome.fromEmployment(taiEmployment(TaxYear.current.previous.startYear), Some("1250L"), None, taxYear)
            )
        )
      ),
      previousEmployments = Some(
        Seq(
          PayeIncomeAudit
            .fromPayeIncome(
              PayeIncome.fromEmployment(taiEmployment2.copy(employmentStatus = Ceased), Some("1199L"), None, taxYear)
            )
        )
      ),
      repayment = None,
      pensions = Some(
        Seq(
          PayeIncomeAudit
            .fromPayeIncome(
              PayeIncome.fromEmployment(taiEmployment3.copy(name = "ALDI", employmentType = PensionIncome), None, None, taxYear)
            )
        )
      ),
      otherIncomes       = Some(otherIncomes.map(otherIncs => OtherIncomeAudit.fromOtherIncome(otherIncs))),
      taxCodeChange      = None,
      simpleAssessment   = None,
      taxFreeAmount      = Some(10000),
      estimatedTaxAmount = Some(250)
    )

  val taxCodeRecord: TaxCodeRecord = TaxCodeRecord(
    taxCode          = "1250L",
    startDate        = LocalDate.now().minusYears(2),
    endDate          = LocalDate.now().minusYears(1),
    employerName     = "TESCO",
    pensionIndicator = false,
    payrollNumber    = Some("1234"),
    primary          = true
  )

  val taxCodeRecord2: TaxCodeRecord = TaxCodeRecord(
    taxCode          = "1199L",
    startDate        = LocalDate.now().minusYears(2),
    endDate          = LocalDate.now().minusYears(1),
    employerName     = "ASDA",
    pensionIndicator = false,
    payrollNumber    = Some("4321"),
    primary          = true
  )

  val employmentData: Seq[Employment] = Seq(
    taiEmployment(TaxYear.current.previous.startYear),
    taiEmployment2.copy(employmentStatus = Ceased),
    taiEmployment3.copy(name             = "ALDI", employmentType = PensionIncome)
  )

  val employmentOnlyData = Seq(
    taiEmployment(TaxYear.current.previous.startYear).copy(annualAccounts = Seq.empty),
    taiEmployment2.copy(employmentStatus                                  = Ceased, annualAccounts = Seq.empty),
    taiEmployment3.copy(name                                              = "ALDI", employmentType = PensionIncome, annualAccounts = Seq.empty)
  )

  val annualAccountsIt = Seq(
    annualAccount(TaxYear.current.previous.startYear, Available, seqNo = 3),
    AnnualAccount(4,
                  TaxYear(startYear = TaxYear.current.back(4).currentYear),
                  Seq(Payment(LocalDate.now().minusDays(63), 50, 20, 10, 30, 5, 2)),
                  Available
                 ),
    AnnualAccount(4, TaxYear(TaxYear.current.back(5).currentYear), Seq(Payment(LocalDate.now().minusDays(63), 50, 20, 10, 30, 5, 2)), Available),
    annualAccount(TaxYear.current.previous.startYear, Available, seqNo = 5)
  )

  val taxCodeData: Seq[TaxCodeRecord] = Seq(taxCodeRecord, taxCodeRecord2)

  val taxCodeChangeDetails: TaxCodeChangeDetails = TaxCodeChangeDetails(Seq(taxCodeRecord), Seq(taxCodeRecord2))
}
