package com.cleanpic.ui

import androidx.compose.ui.Modifier

/** iOS 无 resource-id 概念，testTag 由测试框架直接读取，此处 no-op。 */
actual fun Modifier.enableTestTagsAsResourceId(): Modifier = this
