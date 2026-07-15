package com.example.interviewassistant.feature.interviewassistant.domain.usecase

/**
 * 在文本送往语言模型前替换个人敏感信息（PII），本地存储始终保留原文。
 *
 * 设计要点：
 * - 规则按声明顺序执行，身份证必须先于手机号（18 位号码包含 11 位子串，会被手机号规则误伤）；
 * - 数字类规则使用 (?<!\d) / (?!\d) 边界，避免命中更长数字串中的片段；
 * - 微信 / 生日 / 住址等仅在带标签前缀时命中，降低对普通文本的误伤；
 * - 替换为语义化占位符而非直接删除，保持简历上下文连贯。
 *
 * 已知取舍：无标签的 11 位裸数字（如某些业务指标）可能被当作手机号替换，
 * 隐私场景下选择「宁可多脱、不可漏脱」。
 */
class PrivacyRedactor {
    /**
     * 返回替换敏感信息后的文本；空白文本原样返回。
     *
     * @param text 任意待发送到模型的文本（简历、转写等）。
     */
    fun redact(text: String): String {
        if (text.isBlank()) return text
        return RULES.fold(text) { acc, rule -> rule.regex.replace(acc, rule.replacement) }
    }

    private data class RedactionRule(val regex: Regex, val replacement: String)

    private companion object {
        val RULES = listOf(
            // 身份证（18 位，末位可为 X）——必须排在手机号之前
            RedactionRule(
                Regex("(?<!\\d)[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx](?!\\d)"),
                "[身份证号]",
            ),
            // 大陆手机号，容忍 OCR 产生的空格 / 连字符分隔
            RedactionRule(
                Regex("(?<!\\d)1[3-9]\\d[-\\s]?\\d{4}[-\\s]?\\d{4}(?!\\d)"),
                "[手机号]",
            ),
            // 邮箱
            RedactionRule(
                Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"),
                "[邮箱]",
            ),
            // 微信 / QQ：必须带标签前缀才命中，避免误伤普通字母数字串
            RedactionRule(
                Regex("(微信|WeChat|wx|QQ)\\s*[:：号]?\\s*[A-Za-z0-9_-]{5,20}", RegexOption.IGNORE_CASE),
                "\$1：[已隐藏]",
            ),
            // 出生日期：带标签才命中（裸日期无法与工作经历时间区分）
            RedactionRule(
                Regex("(出生日期|出生年月|生日)\\s*[:：]?\\s*\\d{4}[.年/-]\\s?\\d{1,2}([.月/-]\\s?\\d{1,2}日?)?"),
                "\$1：[已隐藏]",
            ),
            // 住址：带标签，替换到行尾（长度限制防止吞掉后续段落）
            RedactionRule(
                Regex("(现居|住址|家庭住址|居住地|通讯地址)\\s*[:：]?\\s*[^\\n]{4,60}"),
                "\$1：[已隐藏]",
            ),
        )
    }
}
