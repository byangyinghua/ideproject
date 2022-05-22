layui.config({
    base: '../../static/js/'
}).extend({ //设定模块别名
    admin: 'admin',
    server_api: 'server_api'
});

var weeks = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]

var pageData = {
    page: 1,
    pagesize: 10,
    lastPage: 999, //后面计算出来
    total: 0,
    dataList: null
}

var updateStatus = null;


layui.use(['jquery', 'form', 'tree', 'table', 'admin', 'laypage', 'laydate', 'server_api'], function () {
    var $ = layui.jquery,
        admin = layui.admin,
        table = layui.table,
        laypage = layui.laypage,
        laydate = layui.laydate,
        form = layui.form,
        tree = layui.tree,
        server_api = layui.server_api;


    function checkAdminUser() {
        server_api.getUserInfo(null, function (resp) {
            if (resp.status == 0) {
                currentUser = resp.result;
                if (currentUser[0].is_supper == 1) {
                    $("#btnUpdateNow").show()
                    $("#updateStatus").show()
                    showUpdateStatus()
                }
            }
        })
    }


    function showUpdateStatus() {
        var postData = {
            attach_id: "",
            action: "check"
        }

        server_api.appUpdateStatus(JSON.stringify(postData), function (resp) {
            if (resp.status == 0) {
                updateStatus = resp.result;
                var currentUsrCols = [
                    {
                        field: 'apk_name',
                        align: 'left',
                        title: '升级的文件',
                        width: 400,
                        event: "apk_name",
                        templet: function (val) {
                            if (!val.apk_name || val.apk_name == "") {
                                return '<div><span class="layui-btn layui-btn-xs layui-btn-normal">请设置</span></div>';
                            } else {
                                return '<div>' + val.apk_name + '<span class="layui-btn layui-btn-xs layui-btn-normal horizon-margin">修改</span></div>';
                            }
                        }
                    },
                    {
                        field: 'status',
                        align: 'left',
                        title: '升级状态',
                        width: 100,
                        event: "status",
                        templet: function (val) {
                            if (val.status == "on") {
                                return '<span class="layui-btn layui-btn-xs">升级中</span>';
                            } else {
                                return '<span class="layui-btn layui-btn-xs layui-btn-danger">未开启</span>';
                            }

                        }
                    },

                    {field: 'total_terminals', align: 'left', title: '需要升级的总终端数', width: 160},
                    {field: 'success_cnt', align: 'left', title: '升级成功终端数', width: 160,templet:function (val) {
                            return '<span class="layui-btn layui-btn-xs layui-btn-checked">' + val.success_cnt  + '</span>';

                        }},
                    {field: 'failed_cnt', align: 'left', title: '升级失败终端数', width: 160, event: "failed_cnt",templet:function (val) {
                            return '<span class="layui-btn layui-btn-xs layui-btn-danger">' + val.failed_cnt  + '</span>';
                        }},
                    {field: 'notstart_cnt', align: 'left', title: '未开始升级终端数', width: 160},
                    {
                        field: 'rate', align: 'left', title: '升级进度', width: 100, templet: function (val) {
                            if(val.rate <100){
                                return '<span class="layui-btn layui-btn-xs layui-btn-danger">' + val.rate + "%" + '</span>';
                            }else{
                                return '<span class="layui-btn layui-btn-xs layui-btn-checked">' + val.rate + "%" + '</span>';
                            }

                        }
                    },
                    {field: 'create_time', align: 'left', title: '开始时间', cellMinWidth: 140},


                ]
                table.render({
                    id: "updateStatus",//
                    elem: '#updateStatus',//指定表格元素
                    data: updateStatus,  //表格当前页面数据
                    limit: pageData.pagesize,
                    toolbar: "#currentUpdateToolbar",
                    defaultToolbar: [],
                    cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
                    skin: 'line ', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
                    done: function (res, curr, count) {

                    },
                    cols: [currentUsrCols]
                })

            }
        })


    }


    //监听任务表单项修改
    table.on('tool(updateStatus)', function (obj) {
        var data = obj.data;
        var event = obj.event;

        if (event == "status") {
            if (!data.apk_name || data.apk_name.length == 0) {
                layer.msg("请先设置升级文件！", {icon: 0})
                return
            } else {
                var action = ""
                var msg = ""
                if (data.status == "off") {
                    action = "on"
                    msg = "真的启动终端升级任务吗?"
                } else {
                    action = "off"
                    msg = "真的停止终端升级任务吗?"
                }

                layer.confirm(msg, {icon: 3, title: '提示'}, function (index) {
                    var postData = {
                        attach_id: data.attach_id,
                        action: action
                    }
                    server_api.appUpdateStatus(JSON.stringify(postData), function (resp) {
                        if (resp.status == 0) {
                            var updateItem = {}
                            updateItem.status = action
                            obj.update(updateItem)
                            layer.msg("操作成功!", {icon: 1})
                            showUpdateStatus()

                        } else {
                            layer.msg(resp.msg, {icon: 2})
                        }
                    })
                })

            }

        } else if (event == "apk_name") {

            if(data.status =="on"){
                layer.msg("升级中不可以更改升级文件!",{icon:0})
                return
            }

            var postData = {
                page: 1,
                pagesize: 9999,
                file_type: 4,
                getTotal: "yes"
            }
            server_api.getFileList(JSON.stringify(postData),function (resp) {
                if (resp.status == 0) {
                    var allApkFileList = resp.result;
                    var treeDataList = []
                    for (var i = 0; i < allApkFileList.length; i++) {
                        var oneFile = {}
                        oneFile.title = allApkFileList[i].name + "(" + allApkFileList[i].size + ")"
                        oneFile.id = i
                        oneFile.attach_id = allApkFileList[i].attach_id
                        oneFile.size = allApkFileList[i].size
                        oneFile.apk_name = allApkFileList[i].name
                        treeDataList.push(oneFile)
                    }
                    tree.render({
                        id: 'apkFileTree'
                        , elem: '#apkFileTree'
                        , data: treeDataList
                        , isJump: true  //link 为参数匹配
                        , showCheckbox: true
                    });
                    var tmpWinIndex = popWindow("请选择文件", "#setApkFile", ["50%", "50%"])
                    $("#confirmSelectFile").off("click");
                    $("#confirmSelectFile").on("click", function (object) {
                        var checkedData = tree.getChecked('apkFileTree'); //获取选中节点的数据

                        if (checkedData.length == 0) {
                            layer.msg("未选择任何文件!", {icon: 2})
                            return
                        } else if (checkedData.length > 1) {
                            layer.msg("只能选择一个文件!", {icon: 2})
                            return
                        }
                        var updateItem = {}
                        updateItem.attach_id = checkedData[0].attach_id
                        updateItem.apk_name = checkedData[0].apk_name
                        obj.update(updateItem)

                        layer.close(tmpWinIndex)
                    })
                } else {
                    layer.msg("请现在附件管理里面上传zip升级压缩包!", {icon: 0})
                }
            })

        }
    })


    function addOrDelItem(action, number) {
        //var newTotal = 0;
        if (action == "add") {
            pageData.total = Number(pageData.total) + Number(number);
        } else if (action == "del") {
            pageData.total = Number(pageData.total) - Number(number);
        }

        var m = pageData.total / pageData.pagesize;
        var n = pageData.total % pageData.pagesize;
        pageData.lastPage = n > 0 ? m + 1 : n;
    }

    function renderPages(total) {
        laypage.render({
            elem: 'pages'
            , count: total
            , layout: ['count', 'prev', 'page', 'next', 'limit', 'refresh', 'skip']
            , jump: function (obj) {
                pageData.total = total;
                pageData.page = obj.curr;
                pageData.pagesize = obj.limit;
                var m = total / pageData.pagesize;
                var n = total % pageData.pagesize;
                pageData.lastPage = n > 0 ? m + 1 : n;
                if (pageData.total > 0) {
                    setCurrentData();
                }
            }
        });
    }

    function setCurrentData(getTotal, helpStatus, isLast, isFirst) {
        var postData = {
            page: pageData.page,
            pagesize: pageData.pagesize,
            getTotal: getTotal
        }

        if (helpStatus && helpStatus.length > 0) {
            postData.help_status = helpStatus
        }

        if (isLast && isLast == "yes") {
            postData.page = pageData.lastPage;
        } else if (isFirst && isFirst == "yes") {
            postData.page = 1;
        }

        server_api.getUpdateLog(JSON.stringify(postData), function (resp) {
            var dataList = [];
            if (resp.status == 0) {
                dataList = resp.result;
                pageData.dataList = dataList;
                if (getTotal == "yes") {
                    renderPages(resp.total);
                }
            } else {
                renderPages(0);
                layer.msg(resp.msg, {icon: 2});
            }
            renderDataList(dataList);
        });
    }

    function renderDataList(dataList) {
        var colsList = [
            {checkbox: true},
            {field: 'id', align: 'left', title: '序号', width: 80, sort: true},
            {field: 'apk_name', align: 'left', title: '升级的apk文件名', cellMinWidth: 260, sort: true},
            {field: 'new_version', align: 'left', title: '升级的版本', width: 200, sort: true},
            {field: 'op_user', align: 'left', title: '操作人', width: 140, sort: true},
            {field: 'total_terminal_cnt', align: 'center', title: '总终端数', width: 120, sort: true},
            {field: 'ok_terminal_cnt', align: 'center', title: '升级成功终端', width: 160, sort: true,templet:function (val) {

                return '<span class="layui-btn layui-btn-xs layui-btn-checked">' + val.ok_terminal_cnt + '</span>';

                }},
            {field: 'fail_terminal_cnt', align: 'center', title: '升级失败终端', width: 160, sort: true,templet:function (val) {
                    return '<span class="layui-btn layui-btn-xs layui-btn-danger">' + val.fail_terminal_cnt + '</span>';

                }},
            {field: 'create_time', align: 'center', title: '开始时间', width: 160, sort: true},
            {field: 'end_time', align: 'center', title: '结束时间', width: 160, sort: true}
        ]

        var renderObj = {
            id: "updateHistory",//
            elem: '#updateHistory',//指定表格元素
            data: dataList,  //表格当前页面数据
            // height: 'full-20',
            limit: pageData.pagesize,
            toolbar: "#updateHistoryToolbar",
            defaultToolbar: ['filter'],
            cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
            skin: 'line ', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
            done: function (res, curr, count) {
            },
            cols: [colsList]
        }

        table.render(renderObj)
    }


    $("#btnUpdateNow").on("click",function (object) {
        var treeDataList = []
        var itemtId = 0

        if(!updateStatus||!updateStatus[0].apk_name){
            layer.msg("请先进行升级设置!",{icon:2})
            return
        }

        var postData = {
            page: 1,
            pagesize: 10000,
            getTotal: "yes"
        }
        server_api.getTerminalList(JSON.stringify(postData),function (terResp) {
            if (terResp.status == 0) {
                var allTerminalList = terResp.result;
                var tmpMap = {}

                server_api.getTerminalGrp(JSON.stringify(postData),function (resp) {
                    var groupList = resp.result;
                    var oneGroup1 = {}
                    oneGroup1.title = "未分组终端"
                    oneGroup1.id = itemtId++
                    oneGroup1.gid = "ungroup"
                    oneGroup1.disabled = true
                    oneGroup1.nTerminal = 0
                    oneGroup1.offlineCnt = 0
                    oneGroup1.children = []
                    tmpMap["ungroup"] = 0
                    treeDataList.push(oneGroup1)
                    if(groupList){
                        for (var i = 0; i < groupList.length; i++) {
                            var oneGroup = {}
                            oneGroup.title = groupList[i].group_name
                            oneGroup.id = itemtId++
                            oneGroup.gid = groupList[i].gid
                            oneGroup.disabled = true
                            oneGroup.nTerminal = 0
                            oneGroup.offlineCnt =0
                            oneGroup.children = []
                            tmpMap[groupList[i].gid] = i + 1
                            treeDataList.push(oneGroup)
                        }
                    }

                    for (var m = 0; m < allTerminalList.length; m++) {
                        var oneTerminal = {}
                        var tmpTitle = allTerminalList[m].name
                        if(!tmpTitle){
                            tmpTitle = "未命名终端"
                        }
                        oneTerminal.id = itemtId++
                        oneTerminal.terminal_id = allTerminalList[m].terminal_id
                        oneTerminal.ip = allTerminalList[m].ip
                        if (allTerminalList[m].gids && allTerminalList[m].gids.length > 0) {
                            for (var w = 0; w < allTerminalList[m].gids.length; w++) {
                                if(allTerminalList[m].state==0){
                                    //treeDataList[tmpMap[allTerminalList[m].gids[w]]].disabled = true
                                    treeDataList[tmpMap[allTerminalList[m].gids[w]]].offlineCnt++
                                    oneTerminal.disabled = true
                                    oneTerminal.title = tmpTitle + '(' + allTerminalList[m].ip + '|未在线)'
                                }else{
                                    oneTerminal.title = tmpTitle + '(' + allTerminalList[m].ip + ')'
                                    treeDataList[tmpMap[allTerminalList[m].gids[w]]].disabled = false
                                }
                                treeDataList[tmpMap[allTerminalList[m].gids[w]]].children.push(oneTerminal)
                                treeDataList[tmpMap[allTerminalList[m].gids[w]]].nTerminal++

                            }
                        } else {
                            if(allTerminalList[m].state==0){
                                treeDataList[0].disabled = true
                                treeDataList[0].offlineCnt++
                                oneTerminal.disabled = true
                                oneTerminal.title = tmpTitle + '(' + allTerminalList[m].ip + '|未在线)'
                            }else{
                                treeDataList[0].disabled = false
                                oneTerminal.title = tmpTitle + '(' + allTerminalList[m].ip + ')'
                            }
                            treeDataList[0].children.push(oneTerminal)
                            treeDataList[0].nTerminal++
                        }
                    }
                    for(var x=0;x<treeDataList.length;x++){
                        if(treeDataList[x].offlineCnt >0){
                            treeDataList[x].title = treeDataList[x].title + '(未在线:' + treeDataList[x].offlineCnt + '个)'
                        }
                    }

                    tree.render({
                        id: 'terminalGrpTree'
                        , elem: '#terminalGrpTree'
                        , data: treeDataList
                        , isJump: true  //link 为参数匹配
                        , showCheckbox: true
                    });
                })
            }else{
                layer.msg("当前没有终端!",{icon:2})
            }
        })

        var tmpWindIndex = popWindow("请选择终端", "#setTerminals", ["50%", "80%"])
        $("#confirmTerminals").off("click");
        $("#confirmTerminals").on("click", function (object) {
            var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
            var terminal_ips = {}
            var terminalIPList = []

            if (checkedData.length > 0) {
                for (var h = 0; h < checkedData.length; h++) {
                    if (checkedData[h].children.length > 0) {
                        for (var i = 0; i < checkedData[h].children.length; i++) {
                            terminal_ips[checkedData[h].children[i].ip] = 1
                        }
                    }
                }
                for (var terminal_ip in terminal_ips) {
                    terminalIPList.push(terminal_ip)
                }
                var postData = {}
                postData.terminal_ips = terminalIPList
                server_api.appUpdateNow(JSON.stringify(postData), function (resp) {
                    if (resp.status == 0) {
                        layer.msg("已经触发终端升级!", {icon: 1})
                        layer.close(tmpWindIndex)
                    } else {
                        layer.msg(resp.msg, {icon: 2})
                    }
                })
            } else {
                layer.msg("未勾选任何终端!", {icon: 2})
            }
        })

    })


    function popWindow(title, elem, size) {
        var index = layer.open({
            type: 1,
            shade: 0.5,
            shadeClose: true,
            title: title, //不显示标题
            closeBtn: 1,
            resize: false,
            area: size,//['500px', '300px'],
            content: $(elem), //捕获的元素，注意：最好该指定的元素要存放在body最外层，否则可能被其它的相对元素所影响
            yes: function (index, layero) {
                //do something
                console.log("run into yes!")
            },
            cancel: function () {

            }
        });
        return index;
    }

    function refreshUpdateStatus(){
        setTimeout(function () {
            checkAdminUser()
            refreshUpdateStatus()
        },30*1000)

    }


    $(function () {
        checkAdminUser()
        refreshUpdateStatus()
        setCurrentData("yes")
    })


})