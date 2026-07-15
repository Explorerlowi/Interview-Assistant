package com.example.interviewassistant.feature.interviewassistant

import com.example.interviewassistant.feature.interviewassistant.domain.usecase.PrivacyRedactor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrivacyRedactorTest {
    private val redactor = PrivacyRedactor()

    @Test
    fun replacesPlainMobileNumber() {
        val result = redactor.redact("联系电话：13812345678，随时可联系")
        assertEquals("联系电话：[手机号]，随时可联系", result)
    }

    @Test
    fun replacesMobileNumberWithSeparators() {
        assertEquals("电话 [手机号]", redactor.redact("电话 138 1234 5678"))
        assertEquals("电话 [手机号]", redactor.redact("电话 138-1234-5678"))
    }

    @Test
    fun replacesIdCardBeforePhoneRule() {
        // 身份证中间包含形似手机号的 11 位子串，必须整体替换为 [身份证号]
        val result = redactor.redact("身份证号 110101199003074518")
        assertEquals("身份证号 [身份证号]", result)
        assertFalse(result.contains("[手机号]"))
    }

    @Test
    fun replacesIdCardEndingWithX() {
        assertEquals("证件：[身份证号]", redactor.redact("证件：11010119900307451X"))
    }

    @Test
    fun replacesEmail() {
        assertEquals(
            "邮箱：[邮箱]（工作日回复）",
            redactor.redact("邮箱：zhang.san+hr@example.com.cn（工作日回复）"),
        )
    }

    @Test
    fun replacesLabeledWechat() {
        val result = redactor.redact("微信：zhangsan_2024")
        assertEquals("微信：[已隐藏]", result)
    }

    @Test
    fun keepsUnlabeledAlphanumericToken() {
        val text = "熟悉 kafka_2024 集群运维"
        assertEquals(text, redactor.redact(text))
    }

    @Test
    fun replacesLabeledBirthday() {
        assertTrue(redactor.redact("出生年月：1995年3月").contains("[已隐藏]"))
        assertTrue(redactor.redact("生日 1995/03/12").contains("[已隐藏]"))
    }

    @Test
    fun keepsWorkExperienceDates() {
        val text = "2019年3月-2022年5月 就职于某科技公司担任后端工程师"
        assertEquals(text, redactor.redact(text))
    }

    @Test
    fun replacesLabeledAddressUntilEndOfLine() {
        val result = redactor.redact("现居：北京市朝阳区望京街道某小区 1 号楼\n工作经历：")
        assertEquals("现居：[已隐藏]\n工作经历：", result)
    }

    @Test
    fun redactsBareElevenDigitNumberAsKnownTradeOff() {
        // 已知取舍：无标签 11 位裸数字会被当作手机号，宁可多脱不可漏脱
        assertEquals("日均调用 [手机号] 次", redactor.redact("日均调用 13800000000 次"))
    }

    @Test
    fun keepsDigitsInsideLongerNumber(): Unit {
        val text = "订单号 2138123456789012"
        assertEquals(text, redactor.redact(text))
    }

    @Test
    fun returnsBlankTextUnchanged() {
        assertEquals("", redactor.redact(""))
        assertEquals("   ", redactor.redact("   "))
    }

    @Test
    fun keepsTextWithoutPii() {
        val text = "精通 Kotlin 与 Jetpack Compose，主导过 3 个千万级日活项目。"
        assertEquals(text, redactor.redact(text))
    }
}
