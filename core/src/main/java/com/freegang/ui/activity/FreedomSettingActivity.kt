package com.freegang.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.freegang.base.BaseActivity
import com.freegang.ktutils.app.KAppUtils
import com.freegang.ktutils.app.KToastUtils
import com.freegang.ktutils.app.appVersionName
import com.freegang.ktutils.json.getIntOrDefault
import com.freegang.ktutils.json.parseJSONArray
import com.freegang.ui.asDp
import com.freegang.ui.component.FCard
import com.freegang.ui.component.FCardBorder
import com.freegang.ui.component.FMessageDialog
import com.freegang.ui.viewmodel.FreedomSettingVM
import com.freegang.webdav.WebDav
import com.freegang.xpler.HookPackages
import com.freegang.xpler.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class FreedomSettingActivity : BaseActivity() {
    private val model by viewModels<FreedomSettingVM>()

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun TopBarView() {
        var rotate by remember { mutableStateOf(0f) }
        val rotateAnimate by animateFloatAsState(
            targetValue = rotate,
            animationSpec = tween(durationMillis = Random.nextInt(500, 1500)),
        )

        //更新日志弹窗
        var showUpdateLogDialog by remember { mutableStateOf(false) }
        var updateLog by remember { mutableStateOf("") }
        if (showUpdateLogDialog) {
            FMessageDialog(
                title = "更新日志",
                onlyConfirm = true,
                confirm = "确定",
                onConfirm = { showUpdateLogDialog = false },
                content = {
                    LazyColumn(
                        modifier = Modifier,
                        content = {
                            item {
                                SelectionContainer {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = updateLog,
                                        style = MaterialTheme.typography.body1,
                                    )
                                }
                            }
                        },
                    )
                },
            )
        }

        TopAppBar(
            modifier = Modifier.padding(vertical = 24.dp),
            elevation = 0.dp,
            backgroundColor = MaterialTheme.colors.background,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BoxWithConstraints(modifier = Modifier.padding(end = 24.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    onBackPressedDispatcher.onBackPressed()
                                },
                            ),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Freedom+ Setting",
                        style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.onSurface),
                    )
                    Spacer(modifier = Modifier.padding(vertical = 2.dp))
                    Text(
                        text = "No one is always happy.",
                        style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.onSurface.copy(0.5f)),
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_motion),
                    contentDescription = "更新日志",
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotateAnimate)
                        .combinedClickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onLongClick = {
                                lifecycleScope.launch {
                                    updateLog = withContext(Dispatchers.IO) {
                                        val inputStream = mResources.moduleAssets.open("update.log")
                                        val bytes = inputStream.readBytes()
                                        val text = bytes.decodeToString()
                                        inputStream.close()
                                        text
                                    }
                                    showUpdateLogDialog = updateLog.isNotBlank()
                                }
                            },
                            onClick = {
                                rotate = if (rotate == 0f) 360f else 0f
                            },
                        ),
                )
                /*Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { rewardByAlipay() },
                        ),
                    imageVector = Icons.Rounded.FavoriteBorder,
                    contentDescription = "打赏"
                )*/
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
    @Composable
    private fun BodyView() {
        //版本更新弹窗
        var showNewVersionDialog by remember { mutableStateOf(true) }
        val version by model.versionConfig.observeAsState()
        if (version != null) {
            val version = version!!
            if (version.name.compareTo("v${application.appVersionName}") >= 1 && showNewVersionDialog) {
                FMessageDialog(
                    title = "发现新版本 ${version.name}!",
                    confirm = "确定",
                    cancel = "取消",
                    onCancel = {
                        showNewVersionDialog = false
                    },
                    onConfirm = {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(version.browserDownloadUrl),
                            )
                        )
                    },
                    content = {
                        LazyColumn(
                            modifier = Modifier,
                            content = {
                                item {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = version.body,
                                        style = MaterialTheme.typography.body1,
                                    )
                                }
                            },
                        )
                    }
                )
            }
        }

        //重启抖音提示
        var showRestartAppDialog by remember { mutableStateOf(false) }
        if (showRestartAppDialog) {
            if (application.packageName != HookPackages.modulePackageName) {
                FMessageDialog(
                    title = "提示",
                    cancel = "取消",
                    confirm = "重启",
                    onCancel = {
                        showRestartAppDialog = false
                    },
                    onConfirm = {
                        showRestartAppDialog = false
                        model.setVersionConfig(mResources.moduleAssets)
                        KAppUtils.restartApplication(application)
                    },
                    content = {
                        Text(
                            text = "需要重启应用生效, 若未重启请手动重启",
                            style = MaterialTheme.typography.body1,
                        )
                    },
                )
            }
        }

        //清爽模式响应模式
        var showLongPressModeDialog by remember { mutableStateOf(false) }
        if (showLongPressModeDialog) {
            var isLongPressMode by remember { mutableStateOf(model.isLongPressMode.value ?: true) }
            FMessageDialog(
                title = "请选择响应模式",
                confirm = "更改",
                onlyConfirm = true,
                onConfirm = { showLongPressModeDialog = false },
                content = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isLongPressMode,
                                onClick = {
                                    isLongPressMode = true
                                    model.changeLongPressMode(isLongPressMode)
                                },
                            )
                            Text(
                                text = "长按视频上半",
                                style = MaterialTheme.typography.body1,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !isLongPressMode,
                                onClick = {
                                    isLongPressMode = false
                                    model.changeLongPressMode(isLongPressMode)
                                },
                            )
                            Text(
                                text = "长按视频下半",
                                style = MaterialTheme.typography.body1,
                            )
                        }
                    }
                }
            )
        }

        //WebDav配置编辑
        var showWebDavConfigEditorDialog by remember { mutableStateOf(false) }
        if (showWebDavConfigEditorDialog) {
            val webDavHistory = model.webDavHistory.observeAsState(initial = emptySet())
            var showWebDavHistoryMenu by remember { mutableStateOf(false) }
            var host by remember { mutableStateOf(model.webDavHost.value ?: "") }
            var username by remember { mutableStateOf(model.webDavUsername.value ?: "") }
            var password by remember { mutableStateOf(model.webDavPassword.value ?: "") }
            var isWaiting by remember { mutableStateOf(false) }
            FMessageDialog(
                title = {
                    ExposedDropdownMenuBox(
                        expanded = showWebDavHistoryMenu,
                        onExpandedChange = { /*expanded = !expanded*/ },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = "配置WebDav",
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier
                                    .weight(1f),
                            )

                            BoxWithConstraints {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_history),
                                    contentDescription = "WebDav列表",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .combinedClickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = {
                                                if (webDavHistory.value.isEmpty()) {
                                                    KToastUtils.show(application, "没有WebDav历史")
                                                }
                                                showWebDavHistoryMenu = webDavHistory.value.isNotEmpty()
                                            },
                                        ),
                                )

                                ExposedDropdownMenu(
                                    expanded = showWebDavHistoryMenu,
                                    onDismissRequest = { showWebDavHistoryMenu = false },
                                    modifier = Modifier.heightIn(max = 200.dp)
                                ) {
                                    webDavHistory.value.forEach {
                                        Text(
                                            text = it.host,
                                            style = MaterialTheme.typography.body1,
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onClick = {
                                                        showWebDavHistoryMenu = false
                                                        host = it.host
                                                        username = it.username
                                                        password = it.password
                                                    },
                                                    onLongClick = {
                                                        model.removeWebDavConfig(
                                                            WebDav.Config(
                                                                it.host,
                                                                it.username,
                                                                it.password,
                                                            )
                                                        )
                                                        showWebDavHistoryMenu = webDavHistory.value.isNotEmpty()
                                                        KToastUtils.show(application, "删除成功")
                                                    }
                                                )
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                cancel = "取消",
                confirm = "确定",
                isWaiting = isWaiting,
                onCancel = {
                    showWebDavConfigEditorDialog = false
                },
                onConfirm = {
                    val webDavConfig = WebDav.Config(host, username, password)
                    isWaiting = true
                    model.setWebDavConfig(webDavConfig)
                    model.initWebDav { test, msg ->
                        KToastUtils.show(applicationContext, msg)
                        isWaiting = false
                        if (test) {
                            showWebDavConfigEditorDialog = false
                            model.changeIsWebDav(true)
                            model.addWebDavConfig(webDavConfig)
                            return@initWebDav
                        }
                        model.changeIsWebDav(false)
                    }
                },
                content = {
                    Column {
                        FCard(
                            border = FCardBorder(borderWidth = 1.0.dp),
                            content = {
                                BasicTextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 12.dp),
                                    value = host,
                                    maxLines = 1,
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.body1,
                                    decorationBox = { innerTextField ->
                                        if (host.isEmpty()) Text(
                                            text = "http://服务器地址:端口/初始化路径",
                                            style = MaterialTheme.typography.body1.copy(color = Color(0xFF999999)),
                                        )
                                        innerTextField.invoke() //必须调用这行哦
                                    },
                                    onValueChange = {
                                        host = it
                                    },
                                )
                            },
                        )
                        FCard(
                            modifier = Modifier.padding(vertical = 8.dp),
                            border = FCardBorder(borderWidth = 1.0.dp),
                            content = {
                                BasicTextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 12.dp),
                                    value = username,
                                    maxLines = 1,
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.body1,
                                    decorationBox = { innerTextField ->
                                        if (username.isEmpty()) Text(
                                            text = "用户名",
                                            style = MaterialTheme.typography.body1.copy(color = Color(0xFF999999)),
                                        )
                                        innerTextField.invoke() //必须调用这行哦
                                    },
                                    onValueChange = {
                                        username = it
                                    },
                                )
                            },
                        )
                        FCard(
                            border = FCardBorder(borderWidth = 1.0.dp),
                            content = {
                                BasicTextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 12.dp),
                                    value = password,
                                    maxLines = 1,
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.body1,
                                    decorationBox = { innerTextField ->
                                        if (password.isEmpty()) Text(
                                            text = "密码",
                                            style = MaterialTheme.typography.body1.copy(color = Color(0xFF999999)),
                                        )
                                        innerTextField.invoke() //必须调用这行哦
                                    },
                                    onValueChange = {
                                        password = it
                                    }
                                )
                            },
                        )
                    }
                },
            )
        }

        //隐藏Tab关键字编辑
        var showHideTabKeywordsEditorDialog by remember { mutableStateOf(false) }
        if (showHideTabKeywordsEditorDialog) {
            var hideTabKeywords by remember { mutableStateOf(model.hideTabKeywords.value ?: "") }
            FMessageDialog(
                title = "请输入关键字, 用逗号分开",
                cancel = "取消",
                confirm = "确定",
                onCancel = { showHideTabKeywordsEditorDialog = false },
                onConfirm = {
                    showHideTabKeywordsEditorDialog = false
                    model.setHideTabKeywords(hideTabKeywords)
                },
                content = {
                    FCard(
                        border = FCardBorder(borderWidth = 1.0.dp),
                        content = {
                            BasicTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                value = hideTabKeywords,
                                maxLines = 1,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.body1,
                                onValueChange = {
                                    hideTabKeywords = it
                                },
                            )
                        },
                    )
                },
            )
        }

        //开启隐藏Tab关键字, 复确认弹窗
        var showHideTabTipsDialog by remember { mutableStateOf(false) }
        if (showHideTabTipsDialog) {
            FMessageDialog(
                title = "提示",
                cancel = "关闭",
                confirm = "开启",
                onCancel = {
                    showHideTabTipsDialog = false
                    model.changeIsHideTab(false)
                },
                onConfirm = {
                    showHideTabTipsDialog = false
                    showRestartAppDialog = true
                    model.changeIsHideTab(true)
                },
                content = {
                    Text(
                        text = "一旦开启顶部Tab隐藏, 将禁止左右滑动切换, 具体效果自行查看!",
                        style = MaterialTheme.typography.body1,
                    )
                },
            )
        }

        //定时退出
        var showTimedExitSettingDialog by remember { mutableStateOf(false) }
        if (showTimedExitSettingDialog) {
            val times = model.timedExitValue.value?.parseJSONArray()
            var timedExit by remember { mutableStateOf("${times?.getIntOrDefault(0, 10) ?: 10}") }
            var freeExit by remember { mutableStateOf("${times?.getIntOrDefault(1, 3) ?: 3}") }

            KToastUtils.show(this, "建议在3分钟以上~")
            FMessageDialog(
                title = "定时退出时间设置",
                cancel = "取消",
                confirm = "确定",
                onCancel = {
                    showTimedExitSettingDialog = false
                },
                onConfirm = {
                    showTimedExitSettingDialog = false
                    val intTimedExit = timedExit.toIntOrNull()
                    val intFreeExit = freeExit.toIntOrNull()
                    if (intTimedExit == null || intFreeExit == null) {
                        KToastUtils.show(this, "请输入正确的分钟数")
                        return@FMessageDialog
                    }
                    if (intTimedExit < 0 || intFreeExit < 0) {
                        KToastUtils.show(this, "请输入正确的分钟数")
                        return@FMessageDialog
                    }
                    KToastUtils.show(this, "设置成功, 下次启动生效")
                    model.setTimedExitValue("[$timedExit, $freeExit]")
                }
            ) {
                Column {
                    FCard(
                        border = FCardBorder(borderWidth = 1.0.dp),
                        content = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    text = "运行退出",
                                )
                                BasicTextField(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 12.dp),
                                    value = timedExit,
                                    maxLines = 1,
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.body1,
                                    decorationBox = { innerTextField ->
                                        if (timedExit.isEmpty()) Text(
                                            text = "运行超过指定时间",
                                            style = MaterialTheme.typography.body1.copy(color = Color(0xFF999999)),
                                        )
                                        innerTextField.invoke() //必须调用这行哦
                                    },
                                    onValueChange = {
                                        timedExit = it
                                    },
                                )
                                Text(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    text = "分钟",
                                )
                            }
                        },
                    )
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    FCard(
                        border = FCardBorder(borderWidth = 1.0.dp),
                        content = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    text = "空闲退出",
                                )
                                BasicTextField(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 12.dp),
                                    value = freeExit,
                                    maxLines = 1,
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.body1,
                                    decorationBox = { innerTextField ->
                                        if (freeExit.isEmpty()) Text(
                                            text = "空闲超过指定时间",
                                            style = MaterialTheme.typography.body1.copy(color = Color(0xFF999999)),
                                        )
                                        innerTextField.invoke() //必须调用这行哦
                                    },
                                    onValueChange = {
                                        freeExit = it
                                    },
                                )
                                Text(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    text = "分钟",
                                )
                            }
                        },
                    )
                }
            }
        }

        Scaffold(
            modifier = Modifier.padding(horizontal = 24.dp),
            topBar = { TopBarView() }
        ) {
            Box(Modifier.padding(it)) {
                LazyColumn(
                    content = {
                        item {
                            // 选项
                            SwitchItem(
                                text = "视频创作者单独创建文件夹",
                                checked = model.isOwnerDir.observeAsState(false),
                                onCheckedChange = {
                                    model.changeIsOwnerDir(it)
                                },
                            )
                            SwitchItem(
                                text = "视频/图文/音乐下载",
                                checked = model.isDownload.observeAsState(false),
                                onCheckedChange = {
                                    model.changeIsDownload(it)
                                }
                            )
                            SwitchItem(
                                text = "保存表情包/评论视频、图片",
                                checked = model.isEmoji.observeAsState(false),
                                onCheckedChange = {
                                    model.changeIsEmoji(it)
                                }
                            )
                            SwitchItem(
                                text = "震动反馈",
                                checked = model.isVibrate.observeAsState(false),
                                onCheckedChange = {
                                    model.changeIsVibrate(it)
                                }
                            )
                            SwitchItem(
                                text = "首页控件半透明",
                                checked = model.isTranslucent.observeAsState(false),
                                onCheckedChange = {
                                    showRestartAppDialog = true
                                    model.changeIsTranslucent(it)
                                }
                            )
                            SwitchItem(
                                text = "禁用双击点赞",
                                checked = model.isDisableDoubleLike.observeAsState(false),
                                onCheckedChange = {
                                    model.changeIsDisableDoubleLike(it)
                                }
                            )
                            SwitchItem(
                                text = "清爽模式",
                                subtext = "长按视频进入清爽模式, 点击更改响应模式",
                                checked = model.isNeatMode.observeAsState(false),
                                onClick = {
                                    showLongPressModeDialog = true
                                },
                                onCheckedChange = {
                                    model.changeIsNeatMode(it)
                                }
                            )
                            SwitchItem(
                                text = "通知栏下载",
                                subtext = "开启通知栏下载, 否则将显示下载弹窗",
                                checked = model.isNotification.observeAsState(false),
                                onCheckedChange = {
                                    model.changeIsNotification(it)
                                },
                            )
                            var isWebDavWaiting by remember { mutableStateOf(false) }
                            SwitchItem(
                                text = "WebDav",
                                subtext = "点击配置WebDav",
                                isWaiting = isWebDavWaiting,
                                checked = model.isWebDav.observeAsState(false),
                                onClick = {
                                    showWebDavConfigEditorDialog = true
                                },
                                onCheckedChange = {
                                    model.changeIsWebDav(it)
                                    if (it && !model.hasWebDavConfig()) {
                                        showWebDavConfigEditorDialog = true
                                        model.changeIsWebDav(false)
                                        Toast.makeText(applicationContext, "请先进行WebDav配置!", Toast.LENGTH_SHORT).show()
                                        return@SwitchItem
                                    }
                                    if (it) {
                                        isWebDavWaiting = true
                                        model.initWebDav { test, msg ->
                                            KToastUtils.show(applicationContext, msg)
                                            isWebDavWaiting = false
                                            if (test) {
                                                model.changeIsWebDav(true)
                                                return@initWebDav
                                            }
                                            model.changeIsWebDav(false)
                                        }
                                    }
                                },
                            )
                            SwitchItem(
                                text = "隐藏顶部tab",
                                subtext = "点击设置关键字",
                                checked = model.isHideTab.observeAsState(false),
                                onClick = {
                                    showHideTabKeywordsEditorDialog = true
                                },
                                onCheckedChange = {
                                    showRestartAppDialog = true
                                    if (it) {
                                        showHideTabTipsDialog = true
                                    }
                                    model.changeIsHideTab(it)
                                },
                            )
                            SwitchItem(
                                text = "定时退出",
                                subtext = "点击设置退出时间",
                                checked = model.isTimedExit.observeAsState(false),
                                onClick = {
                                    showTimedExitSettingDialog = true
                                },
                                onCheckedChange = {
                                    model.changeIsTimeExit(it)
                                    showRestartAppDialog = true
                                },
                            )
                            SwitchItem(
                                text = "去插件化",
                                subtext = "去掉抖音内部设置，可避免大部分闪退，提高稳定性",
                                checked = model.isDisablePlugin.observeAsState(false),
                                onClick = {

                                },
                                onCheckedChange = {
                                    model.changeIsDisablePlugin(it)
                                    showRestartAppDialog = true
                                },
                            )
                        }
                    },
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun SwitchItem(
        text: String,
        subtext: String = "",
        isWaiting: Boolean = false,
        checked: State<Boolean>,
        onCheckedChange: (checked: Boolean) -> Unit,
        onClick: () -> Unit = {},
        onLongClick: () -> Unit = {},
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .combinedClickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        onClick.invoke()
                    },
                    onLongClick = {
                        onLongClick.invoke()
                    }
                )
                .then(if (subtext.isNotBlank()) Modifier.padding(vertical = 4.dp) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            content = {
                Column(
                    modifier = Modifier.weight(1f),
                    content = {
                        Text(
                            text = text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.body1,
                        )
                        if (subtext.isNotBlank()) {
                            Text(
                                modifier = Modifier.padding(vertical = 2.dp),
                                text = subtext,
                                style = MaterialTheme.typography.body2,
                            )
                        }
                    },
                )
                if (isWaiting) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .wrapContentSize(Alignment.Center)
                            .padding(17.dp), //switch: width = 34.dp
                        contentAlignment = Alignment.Center,
                        content = {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(MaterialTheme.typography.body1.fontSize.asDp),
                            )
                        }
                    )
                } else {
                    Switch(
                        checked = checked.value,
                        onCheckedChange = {
                            onCheckedChange.invoke(it)
                        },
                    )
                }
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoTheme {
                BodyView()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.loadConfig()
        model.checkVersion()
    }

    override fun onPause() {
        super.onPause()
        model.setVersionConfig(mResources.moduleAssets)
    }

    private fun rewardByAlipay() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("alipays://platformapi/startapp?appId=09999988&actionType=toAccount&goBack=NO&amount=3.00&userId=2088022940366251&memo=呐，拿去吃辣条!")
                )
            )
        } catch (e: Exception) {
            //e.printStackTrace()
            Toast.makeText(applicationContext, "谢谢，你未安装支付宝客户端", Toast.LENGTH_SHORT).show()
        }
    }
}