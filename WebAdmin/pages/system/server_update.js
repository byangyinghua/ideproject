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
var renderObj ={};


layui.use(['jquery', 'form', 'tree', 'table', 'admin', 'laypage', 'laydate', 'server_api'], function () {
    var $ = layui.jquery,
        admin = layui.admin,
        table = layui.table,
        laypage = layui.laypage,
        laydate = layui.laydate,
        form = layui.form,
        tree = layui.tree,
        server_api = layui.server_api;

    function doShowUpdate() {
        server_api.getUserInfo(null, function (resp) {
            if (resp.status == 0) {
                currentUser = resp.result;
                if (currentUser[0].is_supper == 1) {
                    $("#btnUpdateNow").show()
                    $("#updateStatus").show()
                    showUpdateInfo()
                }
            }
        })
        server_api.getServerUpdateInfo(null,function (resp) {
            if (resp.status == 0){
                dataList =resp.result
                $("#currentVersion").text("当前后台版本:" + dataList[0].cur_ver + "，当前war文件md5值:" + dataList[0].war_md5)
            }
            renderObj.data = dataList
            table.render(renderObj)
        })
    }

    function showUpdateInfo() {
        var dataList = [{
            zipfile:"",
            description:"",
            action:""
        }]
        var colsList = [
            {field: 'zipfile', event: "zipfile", align: 'left', title: '设置升级zip文件包', width: 460,templet: function (val) {
                    if (!val.zipfile || val.zipfile == "") {
                        return '<div><span class="layui-btn layui-btn-xs layui-btn-normal">请设置</span></div>';
                    } else {
                        return '<div>' + val.zipfile + '<span class="layui-btn layui-btn-xs layui-btn-normal horizon-margin">修改</span></div>';
                    }
                }
            },
            {field: 'description',event:"description", align: 'left', title: '升级说明', width:720,edit: 'text'},
            {field: 'action', align: 'center', title: '操作', cellMinWidth: 100,toolbar: '#updateActions'}
        ]

        renderObj = {
            id: "updateAction",//
            elem: '#updateAction',//指定表格元素
            // data: dataList,  //表格当前页面数据
            // height: 'full-20',
            limit: pageData.pagesize,
            toolbar: "#currentUpdateToolbar",
            defaultToolbar: [],
            skin: 'line ', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
            done: function (res, curr, count) {
            },
            cols: [colsList]
        }
        table.render(renderObj) //防止页面刷新过慢
    }

    //监听任务表单项修改
    table.on('tool(updateAction)', function (obj) {
        var data = obj.data;
        var event = obj.event;

        if(event == "restart"){
            var action = "restart"
            var msg = "真的重启后台服务器吗?该操作将导致需要等待几分钟！"

            layer.confirm(msg, {icon: 3, title: '提示'}, function (index) {
                var postData = {
                    attach_id: data.attach_id,
                    action: action
                }
                layer.close(index)
                var loadingFlag = layer.msg('正在重启中，请稍候……', {icon: 16, shade: 0.01, shadeClose: false, time: 0});
                server_api.setServerUpdate(JSON.stringify(postData), function (resp) {
                    if (resp.status == 0) {
                        var updateItem = {}
                        updateItem.status = action
                        obj.update(updateItem)
                        layer.msg("操作成功!", {icon: 1})
                    } else {
                        layer.msg(resp.msg, {icon: 2})
                    }
                    //layer.close(loadingFlag)
                })
            })

        }else if (event == "startUpdate") { //开始升级
            if (!data.zipfile || data.zipfile.length == 0) {
                layer.msg("请先设置升级文件！", {icon: 0})
                return
            } else {
                var action = "on"
                var msg = "真的开始升级后台服务吗?操作不可中断!"
                var postData = {
                    attach_id: data.attach_id,
                    action: action,
                    description:data.description
                }

                layer.confirm(msg, {icon: 3, title: '提示'}, function (index) {

                    layer.close(index)
                    var loadingFlag = layer.msg('正在升级中，请稍候……', {icon: 16, shade: 0.01, shadeClose: false, time: 0});
                    server_api.setServerUpdate(JSON.stringify(postData), function (resp) {
                        layer.close(loadingFlag)
                        if (resp.status == 0) {
                            var updateItem = {}
                            updateItem.status = action
                            obj.update(updateItem)
                            layer.msg("操作成功!", {icon: 1})
                        } else {
                            layer.msg(resp.msg, {icon: 2})
                        }
                        server_api.userLogout(null,function (resp) {
                            //升级成功后需要重新登录
                        })
                    })
                })
            }

        } else if (event == "zipfile") {

            var postData = {
                page: 1,
                pagesize: 9999,
                file_type: 4,
                getTotal: "yes"
            }
            server_api.getFileList(JSON.stringify(postData),function (resp) {
                if (resp.status == 0) {
                    var allZipFileList = resp.result;
                    var treeDataList = []
                    for (var i = 0; i < allZipFileList.length; i++) {
                        var oneFile = {}
                        oneFile.title = allZipFileList[i].name + "(" + allZipFileList[i].size + ")"
                        oneFile.id = i
                        oneFile.attach_id = allZipFileList[i].attach_id
                        oneFile.size = allZipFileList[i].size
                        oneFile.zipfile = allZipFileList[i].name
                        treeDataList.push(oneFile)
                    }
                    tree.render({
                        id: 'zipFileTree'
                        , elem: '#zipFileTree'
                        , data: treeDataList
                        , isJump: true  //link 为参数匹配
                        , showCheckbox: true
                    });
                    var tmpWinIndex = popWindow("请选择文件", "#setUpdateFile", ["50%", "50%"])
                    $("#confirmSelectFile").off("click");
                    $("#confirmSelectFile").on("click", function (object) {
                        var checkedData = tree.getChecked('zipFileTree'); //获取选中节点的数据

                        if (checkedData.length == 0) {
                            layer.msg("未选择任何文件!", {icon: 2})
                            return
                        } else if (checkedData.length > 1) {
                            layer.msg("只能选择一个文件!", {icon: 2})
                            return
                        }
                        var updateItem = {}
                        updateItem.attach_id = checkedData[0].attach_id
                        updateItem.zipfile = checkedData[0].zipfile
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

        server_api.getServerUpateHistory(JSON.stringify(postData), function (resp) {
            var dataList = [];
            if (resp.status == 0) {
                dataList = resp.result;
                pageData.dataList = dataList;
                if (getTotal == "yes") {
                    renderPages(resp.total);
                }
            } else {
                renderPages(0);
                //layer.msg(resp.msg, {icon: 2});
            }
            renderDataList(dataList);
        });
    }

    function renderDataList(dataList) {
        var colsList = [
            {checkbox: true},
            {field: 'id', align: 'left', title: '序号', width: 80, sort: true},
            {field: 'filename', align: 'left', title: '升级的zip名字', width: 260, sort: true},
            {field: 'description', align: 'left', title: '升级说明', width: 460, sort: true},
            {field: 'war_md5', align: 'left', title: 'war包md5值', width: 260, sort: true},
            {field: 'version', align: 'center', title: '升级的版本号', width: 160, sort: true},
            {field: 'status', align: 'center', title: '是否升级成功', width: 160, sort: true,templet:function (val) {
                　　　if(val.status==0) {
                       return '<span class="layui-btn layui-btn-xs layui-btn-danger">失败</span>';
                   }else{
                       return '<span class="layui-btn layui-btn-xs layui-btn-normal">成功</span>';
                   }
                }},
            {field: 'create_time', align: 'center', title: '升级时间', cellMinWidth: 100, sort: true}
        ]

        var renderObj1 = {
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

        table.render(renderObj1)
    }


    //监听单元格编辑
    table.on('edit(updateAction)', function(obj){
        var value = obj.value //得到修改后的值
            ,data = obj.data //得到所在行所有键值
            ,field = obj.field; //得到字段
        var data = obj.data;
        var postData = data;

        if(field=="description"){
            //console.log("run into modify task_name")
            var newdata = {};
            newdata[field] = value;
            obj.update(newdata);
        }


    });


    $("#btnUpdateNow").on("click",function (object) {
        var treeDataList = []
        var itemtId = 0
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
                    oneGroup1.offlineCnt =0
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
                layer.msg("未勾选任何终端!", {icon: 0})
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

    $(function () {
        doShowUpdate()
        setCurrentData("yes")
    })


})