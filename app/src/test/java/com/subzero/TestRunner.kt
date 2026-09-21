package com.subzero

import com.subzero.ai.RiskParserTest
import com.subzero.ai.Stage1RegexTest
import com.subzero.data.MandateRepositoryTest
import com.subzero.security.SecurityPolicyTest
import com.subzero.services.BankNotificationParserTest
import com.subzero.services.ReminderSchedulerTest
import com.subzero.services.SmsIngestionEngineTest
import com.subzero.services.UniversalRevocationManagerTest
import com.subzero.engine.AutoPaySyncEngineTest

/**
 * Autonomous test runner to execute and verify all SubZero unit and security test suites.
 */
object TestRunner {

    @JvmStatic
    fun main(args: Array<String>) {
        println("==================================================")
        println("       SUBZERO TEST SUITE VERIFICATION PASS       ")
        println("==================================================")

        var totalPassed = 0
        var totalFailed = 0

        fun runTest(suiteName: String, testName: String, block: () -> Unit) {
            try {
                block()
                println("  [PASS] $suiteName > $testName")
                totalPassed++
            } catch (e: Throwable) {
                println("  [FAIL] $suiteName > $testName: ${e.message}")
                e.printStackTrace()
                totalFailed++
            }
        }

        // Suite 1: Stage 1 Regex Gate
        println("\n--- [1/8] Stage 1 Regex Gatekeeper Tests ---")
        val stage1Suite = Stage1RegexTest()
        runTest("Stage1RegexTest", "testStage1Regex_positiveMatches") { stage1Suite.testStage1Regex_positiveMatches() }
        runTest("Stage1RegexTest", "testStage1Regex_negativeControls") { stage1Suite.testStage1Regex_negativeControls() }
        runTest("Stage1RegexTest", "testStage1Regex_executionSpeedUnder2ms") { stage1Suite.testStage1Regex_executionSpeedUnder2ms() }

        // Suite 2: AI Risk Parser & Fallback
        println("\n--- [2/8] AI Risk Parser & Heuristic Fallback Tests ---")
        val riskSuite = RiskParserTest()
        runTest("RiskParserTest", "testParseModelResponse_validStructuredOutput") { riskSuite.testParseModelResponse_validStructuredOutput() }
        runTest("RiskParserTest", "testParseModelResponse_safeOutput") { riskSuite.testParseModelResponse_safeOutput() }
        runTest("RiskParserTest", "testParseModelResponse_outOfBoundsScoreClamping") { riskSuite.testParseModelResponse_outOfBoundsScoreClamping() }
        runTest("RiskParserTest", "testParseModelResponse_malformedOutputFallback") { riskSuite.testParseModelResponse_malformedOutputFallback() }
        runTest("RiskParserTest", "testPerformDeterministicHeuristicEvaluation_trialAndAutoPay") { riskSuite.testPerformDeterministicHeuristicEvaluation_trialAndAutoPay() }

        // Suite 3: Bank & UPI Notification Parser
        println("\n--- [3/8] Bank & UPI AutoPay Notification Parser Tests ---")
        val notifSuite = BankNotificationParserTest()
        runTest("BankNotificationParserTest", "testParseMandateFromText_validHdfcNotification") { notifSuite.testParseMandateFromText_validHdfcNotification() }
        runTest("BankNotificationParserTest", "testParseMandateFromText_validUpiAutoPayNotification") { notifSuite.testParseMandateFromText_validUpiAutoPayNotification() }
        runTest("BankNotificationParserTest", "testParseMandateFromText_quarterlyMandate") { notifSuite.testParseMandateFromText_quarterlyMandate() }
        runTest("BankNotificationParserTest", "testParseMandateFromText_annualMandate") { notifSuite.testParseMandateFromText_annualMandate() }
        runTest("BankNotificationParserTest", "testParseMandateFromText_nonMandateNotificationIgnored") { notifSuite.testParseMandateFromText_nonMandateNotificationIgnored() }

        // Suite 4: Security Policy & Protected Packages
        println("\n--- [4/8] Security Boundary & Fail-Closed Tests ---")
        val secSuite = SecurityPolicyTest()
        runTest("SecurityPolicyTest", "testProtectedPackages_blocksPhonePe") { secSuite.testProtectedPackages_blocksPhonePe() }
        runTest("SecurityPolicyTest", "testProtectedPackages_blocksGooglePay") { secSuite.testProtectedPackages_blocksGooglePay() }
        runTest("SecurityPolicyTest", "testProtectedPackages_blocksPaytm") { secSuite.testProtectedPackages_blocksPaytm() }
        runTest("SecurityPolicyTest", "testProtectedPackages_blocksBhimAndBanking") { secSuite.testProtectedPackages_blocksBhimAndBanking() }
        runTest("SecurityPolicyTest", "testProtectedPackages_failClosedOnNullOrBlank") { secSuite.testProtectedPackages_failClosedOnNullOrBlank() }
        runTest("SecurityPolicyTest", "testProtectedPackages_allowsGenericThirdPartyApp") { secSuite.testProtectedPackages_allowsGenericThirdPartyApp() }
        runTest("SecurityPolicyTest", "testOverlayFlags_requiresNotTouchableAndNotFocusable") { secSuite.testOverlayFlags_requiresNotTouchableAndNotFocusable() }

        // Suite 5: Mandate Repository & Monthly Liability
        println("\n--- [5/8] Mandate Liability & Normalization Tests ---")
        val repoSuite = MandateRepositoryTest()
        runTest("MandateRepositoryTest", "testMandateEntity_monthlyDrainCalculations") { repoSuite.testMandateEntity_monthlyDrainCalculations() }
        runTest("MandateRepositoryTest", "testMandateEntity_revokedMandateZeroDrain") { repoSuite.testMandateEntity_revokedMandateZeroDrain() }
        runTest("MandateRepositoryTest", "testMandateEntity_maskedUmn") { repoSuite.testMandateEntity_maskedUmn() }

        // Suite 6: Retroactive Regulatory SMS Ingestion Engine
        println("\n--- [6/8] TRAI SMS Ingestion Engine Tests ---")
        val smsSuite = SmsIngestionEngineTest()
        runTest("SmsIngestionEngineTest", "testIsRegulatorySender_traiWhitelistedHeaders") { smsSuite.testIsRegulatorySender_traiWhitelistedHeaders() }
        runTest("SmsIngestionEngineTest", "testIsRegulatorySender_rejectsPersonalMobileNumbers") { smsSuite.testIsRegulatorySender_rejectsPersonalMobileNumbers() }
        runTest("SmsIngestionEngineTest", "testIsRegulatorySender_rejectsUnknownOrChatHeaders") { smsSuite.testIsRegulatorySender_rejectsUnknownOrChatHeaders() }
        runTest("SmsIngestionEngineTest", "testIsAutoPayRegulatoryMessage_rejectsOtps") { smsSuite.testIsAutoPayRegulatoryMessage_rejectsOtps() }
        runTest("SmsIngestionEngineTest", "testParseRegulatorySms_validMandateExtracted") { smsSuite.testParseRegulatorySms_validMandateExtracted() }
        runTest("SmsIngestionEngineTest", "testParseRegulatorySms_ignoresNonMandateFinancialMessage") { smsSuite.testParseRegulatorySms_ignoresNonMandateFinancialMessage() }

        // Suite 7: Predictive 48-Hour Pre-Debit Alarm Scheduler
        println("\n--- [7/8] Predictive 48-Hour Alarm Scheduler Tests ---")
        val reminderSuite = ReminderSchedulerTest()
        runTest("ReminderSchedulerTest", "testPreDebitOffset_isExactFortyEightHours") { reminderSuite.testPreDebitOffset_isExactFortyEightHours() }
        runTest("ReminderSchedulerTest", "testIntervalMs_correctlyNormalizesFrequencies") { reminderSuite.testIntervalMs_correctlyNormalizesFrequencies() }
        runTest("ReminderSchedulerTest", "testCalculateReminderTimestamp_futureDebitFiresAtExact48HoursBefore") { reminderSuite.testCalculateReminderTimestamp_futureDebitFiresAtExact48HoursBefore() }
        runTest("ReminderSchedulerTest", "testCalculateReminderTimestamp_nearDebitFiresImmediatelyWithinWindow") { reminderSuite.testCalculateReminderTimestamp_nearDebitFiresImmediatelyWithinWindow() }

        // Suite 8: Universal Revocation Queue Manager
        println("\n--- [8/9] Universal Revocation Queue Lifecycle Tests ---")
        val queueSuite = UniversalRevocationManagerTest()
        runTest("UniversalRevocationManagerTest", "testStartQueue_initializesActiveMandatesOnly") { queueSuite.testStartQueue_initializesActiveMandatesOnly() }
        runTest("UniversalRevocationManagerTest", "testAdvanceQueue_transitionsToNextStepAndCompletes") { queueSuite.testAdvanceQueue_transitionsToNextStepAndCompletes() }
        runTest("UniversalRevocationManagerTest", "testClearQueue_resetsStateCleanly") { queueSuite.testClearQueue_resetsStateCleanly() }

        // Suite 9: Multi-App AutoPay Ingestion & UPI Source Resolution
        println("\n--- [9/9] Multi-App AutoPay Ingestion & UPI Source Tests ---")
        val syncSuite = AutoPaySyncEngineTest()
        runTest("AutoPaySyncEngineTest", "testBankingSenderPattern_matchesRegulatoryHeaders") { syncSuite.testBankingSenderPattern_matchesRegulatoryHeaders() }
        runTest("AutoPaySyncEngineTest", "testMandateExtraction_creationAndAmounts") { syncSuite.testMandateExtraction_creationAndAmounts() }
        runTest("AutoPaySyncEngineTest", "testRevocationPattern_detectsRevokedMandates") { syncSuite.testRevocationPattern_detectsRevokedMandates() }
        runTest("AutoPaySyncEngineTest", "testResolveUpiAppSource_identifiesEcosystems") { syncSuite.testResolveUpiAppSource_identifiesEcosystems() }
        runTest("AutoPaySyncEngineTest", "testMandateEntity_fullDataIntegrity") { syncSuite.testMandateEntity_fullDataIntegrity() }

        println("\n==================================================")
        println("SUMMARY: $totalPassed PASSED, $totalFailed FAILED (TOTAL: ${totalPassed + totalFailed})")
        println("==================================================")

        if (totalFailed > 0) {
            System.exit(1)
        }
    }
}
