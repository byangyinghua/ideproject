layui.config({
    base: '../../static/js/'
}).extend({ //设定模块别名
    admin: 'admin',
    server_api: 'server_api',
    common_api: 'common_api'
});
var weeks = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]
var pageData = {
    page: 1,
    pagesize: 10,
    lastPage: 999, //后面计算出来
    total: 0,
    dataList: null,
}

var newDefaultShieldTask ={
    task_name:"新的屏蔽任务",
    start_time:"2019-09-03 00:00",
    end_time:"2019-09-03 00:00",
    terminals:[""],//每天循环
    week_days:[1,2,3,4,5,6,7],
    state:0
}

var TerminalGrpList =null
var AllTerminalList =null


layui.use(['jquery', 'form', 'tree', 'table', 'transfer', 'util', 'admin', 'laypage', 'laydate', 'server_api','common_api'], function () {
    var $ = layui.jquery,
        admin = layui.admin,
        table = layui.table,
        laypage = layui.laypage,
        laydate = layui.laydate,
        form = layui.form,
        transfer = layui.transfer,
        util = layui.util,
        tree = layui.tree,
        common_api =layui.common_api,
        server_api = layui.server_api;

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

    function addOrDelItem(action, number) {
        if (action == "add") {
            pageData.total = pageData.total + number;
        } else if (action == "del") {
            pageData.total = pageData.total - number;
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

    function setCurrentData(getTotal, isLast, isFirst) {
        var postData = {
            page: pageData.page,
            pagesize: pageData.pagesize,
            plan_type: "normal",
            getTotal: getTotal
        }

        if (isLast && isLast == "yes") {
            postData.page = pageData.lastPage;
        } else if (isFirst && isFirst == "yes") {
            postData.page = 1;
        }

        server_api.getShieldTasks(JSON.stringify(postData), function (resp) {
            var dataList = [];
            if (resp.status == 0) {
                //refreshCurrentPage(table,refreshResp.result);
                dataList = resp.result;
                //console.log(resp.result)
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
        table.render({
            id: "shieldTaskList",//
            elem: '#shieldTaskList',//指定表格元素
            data: dataList,  //表格当前页面数据
            // height: 'full-20',
            limit: pageData.pagesize,
            toolbar: '#toolbarAction',
            defaultToolbar: ['filter'],
            cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
            skin: 'line ', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
            done: function (res, curr, count) {
                $(".layui-table-main  tr").each(function (index, val) {
                    $($(".layui-table-fixed .layui-table-body tbody tr")[index]).height($(val).height());
                });
            },
            // parseData: function (res) { //res 即为原始返回的数据
            //     console.log("run into rable parse data");
            //     console.log(res)
            //     return res
            // },
            cols: [[
                {checkbox: true},
                {field: 'id', align: 'left', title: '序号', width: 80, sort: true},
                {title: '操作', toolbar: '#shieldActions', width: 80},
                {field: 'shield_id', align: 'left', title: '屏蔽任务id', width: 240, sort: true},
                {field: 'task_name', align: 'left', title: '任务名称(可编辑)', width: 280,edit:true,event:"task_name"},
                {field: 'start_time', align: 'left', title: '开始时间', width: 180, sort: true, event: "start_time"},
                {field: 'end_time', align: 'left', title: '解除时间', width: 180, sort: true, event: "end_time"},
                {
                    field: 'week_days',
                    align: 'left',
                    title: '屏蔽的工作日',
                    width: 320,
                    sort: true,
                    event: "week_days",
                    templet: function (val) {
                        var showStr = ""
                        var weekDays = val.week_days
                        if (weekDays && weekDays.length > 0) {
                            for (var i = 0; i < weekDays.length; i++) {
                                if (showStr.length == 0) {
                                    showStr += weeks[weekDays[i] - 1]
                                    // console.log("0:",weeks[weekDays[i] - 1])
                                } else {
                                    showStr += "，" + weeks[weekDays[i] - 1]
                                    //console.log(weekDays[i],i,weeks[weekDays[i] - 1])
                                }
                            }
                            return showStr
                        } else {
                            return "未设置"
                        }
                    }
                },
                {field: 'ready_terminals', align: 'center', title: '未下发终端', width: 160, sort: true,event:"ready_terminals",templet:function (val) {
                        if(val.ready_terminals && val.ready_terminals.length >0){
                            return '<span class="layui-btn layui-btn-xs layui-btn-normal">' + val.ready_terminals.length +'个(点击下发)</span>';
                        }else{
                            return '<span class="layui-btn layui-btn-xs layui-btn-normal">点击配置</span>';
                        }
                    }},
                {field: 'ok_terminals', align: 'center', title: '已启用终端', width: 160,event:"ok_terminals",templet:function (val) {
                        return  '<span class="layui-btn layui-btn-xs layui-btn-checked">'+val.ok_terminals.length　+'个(点击停用)</span>';
                    }},
                {field: 'fail_terminals', align: 'center', title: '已失败终端', width: 160,event:"fail_terminals",templet:function (val) {
                        return '<span class="layui-btn layui-btn-xs layui-btn-danger">'+val.fail_terminals.length +'个(点击查看)</span>';
                    }},
                {field: 'creator', align: 'center', title: '创建人',width:140},
                {field: 'update_time', align: 'center',   width: 180, title: '创建时间', templet: function (val) {
                        return val.update_time.substring(0,19)
                    }
                }

            ]]
        });
    }


    //监听单元格编辑
    table.on('edit(shieldTaskList)', function(obj){
        var value = obj.value //得到修改后的值
            ,data = obj.data //得到所在行所有键值
            ,field = obj.field; //得到字段
        var data = obj.data;
        var postData = data;
        if(data.ok_terminals.length > 0){
            layer.msg("任务存在启用的终端，不可编辑!",{icon:0})
            setCurrentData()
            return
        }
        if(field=="task_name"){
            postData.task_name = value;
            server_api.addOrUpdateShieldTask(JSON.stringify(postData), function (resp) {
                if(resp.status==0){
                    var newdata = {};
                    newdata[field] = value;
                    obj.update(newdata);
                    layer.msg("修改任务名成功!",{icon:1})
                }else{
                    layer.msg("修改任务名失败!",{icon:2})
                }

            });
        }


    });


    table.on('toolbar(shieldTaskList)', function (obj) {
        if(obj.event == "addTaskBtn"){
            server_api.addOrUpdateShieldTask(JSON.stringify(newDefaultShieldTask),function (resp) {
                if(resp.status==0){
                    layer.msg("添加屏蔽任务成功，请在列表中进行修改!",{icon:1})
                    addOrDelItem("add", 1)
                    setCurrentData("yes")
                }else{
                    layer.msg("新增屏蔽任务失败!",{icon:2})
                }
            })

        }else if (obj.event == "batchDelTask") {
            var data = table.checkStatus('shieldTaskList').data;
            if (data && data.length > 0) {
                var shield_ids = []
                for (var i = 1; i <= data.length; i++) {
                    if (data[i-1].ok_terminals.length >0) {
                        layer.msg("第" + i + "个任务存在启用的终端,不可删除!", {icon: 0})
                        return
                    }
                    shield_ids.push(data[i-1].shield_id)
                }
                layer.confirm('是否真的删除勾选的任务?', {icon: 3, title: '提示'}, function (index) {
                    var postData = {
                        shield_ids: shield_ids
                    }
                    server_api.deleteShieldTask(JSON.stringify(postData), function (resp) {
                        if (resp.status == 0) {
                            //obj.del();
                            layer.msg("删除成功!", {icon: 1})
                            addOrDelItem("del", data.length)
                            setCurrentData("yes")
                        } else {
                            layer.msg(resp.msg, {icon: 2})
                        }
                    })
                });

            } else {
                layer.msg("未勾选任务任务!", {icon: 0})
            }

        }

    })
    table.on('tool(shieldTaskList)', function (obj) {
        var data = obj.data;
        var event = obj.event;
        if(event =="deltask"){
            if(data.ok_terminals.length >0){
                layer.msg("任务存在启用的终端,不可删除!", {icon: 0})
            } else{
                layer.confirm('是否真的删除此屏蔽任务?', {icon: 3, title: '提示'}, function (index) {
                    var shield_ids =[data.shield_id]
                    var postData ={
                        shield_ids:shield_ids
                    }
                    server_api.deleteShieldTask(JSON.stringify(postData),function (resp) {
                        if(resp.status==0){
                            layer.msg("删除屏蔽任务成功!",{icon:1})
                            addOrDelItem("del",1)
                            setCurrentData("yes")
                        }else{
                            layer.msg("删除屏蔽任务失败!",{icon:2})
                        }
                    })
                })
            }
        }else if (event == "start_time" || event == "end_time") {
            if(data.ok_terminals.length >0){
                layer.msg("任务存在启用的终端，不可修改!",{icon:0})
                return
            }else{
                laydate.render({
                    elem: this.firstChild
                    , type: 'datetime'
                    , show: true //直接显示
                    , closeStop: this
                    ,format:"yyyy-MM-dd HH:mm"
                    , done: function (value, date) {
                        var newdata = {};
                        if (!value || event.length == 0) {
                            layer.msg("必须设置时间!");
                            newdata[event] = data[event];
                            obj.update(newdata);
                            return;
                        } else if (value != data[event]) {
                            var postData = data;
                            postData[event] = value;
                            var isPass = common_api.diffWithToday(data[event].split(" ")[0])
                            if(isPass <0){
                                layer.msg("请注意日期不能早于今日!",{icon:0})
                                newdata[event] = data[event];
                                obj.update(newdata);
                                return;
                            }if(postData.start_time >= postData.end_time){
                                layer.msg("结束时间必须大于开始时间!",{icon:2})
                                newdata[event] = data[event];
                                obj.update(newdata);
                                return;
                            }else{
                                server_api.addOrUpdateShieldTask(JSON.stringify(postData), function (resp) {
                                    if (resp.status == 0) {
                                        layer.msg("修改时间成功!", {icon: 1})
                                        newdata[event] = value;
                                        obj.update(newdata);
                                    } else {
                                        layer.msg("修改时间失败!", {icon: 2})
                                    }
                                });
                            }
                        }
                    }

                });
            }


        } else if (event == "week_days") {
            if(data.ok_terminals.length >0){
                layer.msg("任务存在启用的终端，不可修改!",{icon:0})
                return
            }
            $("#weekChoiceList").empty()
            var weekChoiceList = $("#weekCheckForm").find("[id=weekChoiceList]")
            for (var i = 0; i < weeks.length; i++) {
                var newInput = $("<input>")
                newInput.attr("type", "checkbox")
                newInput.attr("name", "week-" + data.id + "-" + i)
                newInput.attr("title", weeks[i])
                if(data.week_days){
                    for (var j = 0; j < data.week_days.length; j++) {
                        if (data.week_days[j] == i + 1) {
                            newInput.attr("checked", true)
                            break
                        }
                    }
                }

                weekChoiceList.append(newInput)
            }
            form.render();
            var tmpWinIndex = popWindow("请选择每周播放日", "#weekCheck", ["800px", "150px"])
            $("#submit-week").off("click");
            $("#submit-week").on("click", function (submitWeekDays) {
                var week_days = []
                var checkedWeekDay = $("#weekCheckForm").find("[name^=week-]:checked")
                if (checkedWeekDay.length == 0) {
                    layer.msg("至少选择一个！", {icon: 0})
                    return
                }
                for (var i = 0; i < checkedWeekDay.length; i++) {
                    var tmpId = $(checkedWeekDay[i])[0].name.split("-")[2]
                    week_days.push(parseInt(tmpId) + 1)
                }

                var postData = data;
                postData.week_days = week_days;
                server_api.addOrUpdateShieldTask(JSON.stringify(postData), function (resp) {
                    if (resp.status == 0) {
                        var newdata = {}
                        newdata.week_days = week_days;
                        obj.update(newdata);
                        layer.msg("设置成功!", {icon: 1})
                        layer.close(tmpWinIndex)
                    } else {
                        layer.msg(resp.msg, {icon: 2})
                    }
                });
            })
        }else if(event=="ready_terminals"){
            var oldTerminals = data.ready_terminals
            var treeDataList = []
            if (AllTerminalList != null) {
                var allTerminalList = AllTerminalList;
                var tmpIndex = popWindow("请选择终端", "#setTerminals", ["50%", "80%"])
                if (TerminalGrpList != null) {
                    var groupList = TerminalGrpList;
                    treeDataList =  common_api.buildTerminalTree(groupList,allTerminalList,oldTerminals,true)
                    tree.render({
                        id: 'terminalGrpTree'
                        , elem: '#terminalGrpTree'
                        , data: treeDataList
                        , isJump: true  //link 为参数匹配
                        , showCheckbox: true
                    });

                    $("#terminalGrpTree").append($('  <div  class="align-center verical-margin">\n' +
                        '        <div id="saveReadTerminals" class="layui-btn">保存配置</div>\n' +
                        '        <div id="saveTerminalAndSend" class="layui-btn">立即下发</div>\n' +
                        '    </div>'))
                    $("#saveReadTerminals").off("click");
                    $("#saveReadTerminals").on("click", function (object) {
                        var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
                        var terminalIdList = common_api.getCheckTermialIds(checkedData)
                        if (terminalIdList.length > 0) {
                            var postData = {
                                shield_id:data.shield_id,
                                ready_terminals:terminalIdList
                            }
                            server_api.setShieldTerminals(JSON.stringify(postData), function (resp) {
                                if (resp.status == 0) {
                                    layer.msg("保存配置成功!", {icon: 1})
                                    //layer.close(winIndex)
                                    var newdata = {}
                                    newdata.ready_terminals = terminalIdList;
                                    obj.update(newdata);
                                    layer.close(tmpIndex)
                                } else {
                                    layer.msg(resp.msg, {icon: 2})
                                }
                            })
                        } else {
                            layer.msg("未勾选任何终端!", {icon: 0})
                        }
                    })

                    $("#saveTerminalAndSend").off("click");
                    $("#saveTerminalAndSend").on("click", function (object) {

                        var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
                        var terminalIdList = common_api.getCheckTermialIds(checkedData)

                        if (terminalIdList.length > 0) {
                            var isPass = common_api.diffWithToday(data.end_time.split(" ")[0])
                            if(isPass <0){
                                layer.msg("任务日期已经过期！",{icon:0})
                                return
                            }
                            var postData2={
                                shield_id:data.shield_id,
                                terminal_type:"ready",
                                terminal_ids:terminalIdList
                            }
                            var loadingFlag = layer.msg('正在下发任务，请稍候……', {icon: 16, shade: 0.01, shadeClose: false, time: 0});
                            server_api.sendShielTask2Terminals(JSON.stringify(postData2),function (resp) {
                                layer.close(loadingFlag)
                                if(resp.status==0){
                                    var failedCnt = resp.result[0].failedCnt
                                    if(failedCnt >0){
                                        layer.msg("发送失败终端数:"+failedCnt, {icon: 0})
                                    }else{
                                        layer.msg("保存并发送成功!", {icon: 1})
                                    }
                                    var tmpPostData ={shield_id:data.shield_id}
                                    server_api.getTaskByShieldId(JSON.stringify(tmpPostData),function (resp) {
                                        if(resp.status==0){
                                            var updateData = resp.result[0]
                                            delete updateData.id
                                            obj.update(updateData);
                                        }
                                    })
                                }else{
                                    layer.msg(resp.msg, {icon: 2})
                                }
                            })
                            var newdata = {}
                            newdata.ready_terminals = terminalIdList;
                            obj.update(newdata);
                            layer.close(tmpIndex)
                        } else {
                            layer.msg("未勾选任何终端!", {icon: 0})
                        }
                        //console.log(checkedData);
                    })

                }else{
                    layer.msg("没有获取到终端分组列表!",{icon:2})
                    getTerminalAndGroup()
                }
            }else{
                layer.msg("没有获取到终端列表!",{icon:2})
                getTerminalAndGroup()
            }
        }else if(event=="ok_terminals"){
            var oldTerminals = data.ok_terminals
            if(!oldTerminals||oldTerminals.length==0){
                layer.msg("没有启用的终端!",{icon:0})
                return
            }
            var treeDataList = []
            if (AllTerminalList != null) {
                var allTerminalList = AllTerminalList;
                var tmpIndex = popWindow("请选择终端", "#setTerminals", ["50%", "80%"])
                if (TerminalGrpList != null) {
                    var groupList = TerminalGrpList;
                    treeDataList =  common_api.buildTerminalTree(groupList,allTerminalList,oldTerminals,false)
                    tree.render({
                        id: 'terminalGrpTree'
                        , elem: '#terminalGrpTree'
                        , data: treeDataList
                        , isJump: true  //link 为参数匹配
                        , showCheckbox: true
                    });

                    $("#terminalGrpTree").append($('  <div  class="align-center verical-margin">\n' +
                        '        <div id="deleteTaskFromTerminals" class="layui-btn layui-btn-danger">在勾选的终端上删除此任务</div>\n' +
                        '</div>'))
                    $("#deleteTaskFromTerminals").off("click");
                    $("#deleteTaskFromTerminals").on("click", function (object) {
                        var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
                        var terminalIdList = common_api.getCheckTermialIds(checkedData)
                        if (terminalIdList.length > 0) {
                            var postData = {
                                shield_id:data.shield_id,
                                terminal_ids:terminalIdList
                            }
                            var loadingFlag = layer.msg('正在停止任务，请稍候……', {icon: 16, shade: 0.01, shadeClose: false, time: 0});
                            server_api.clearShieldSetting(JSON.stringify(postData), function (resp) {
                                layer.close(loadingFlag)
                                if (resp.status == 0) {
                                    if(resp.failedCnt >0){
                                        layer.msg("有"+resp.failedCnt +"终端停止失败!", {icon: 1})
                                    }else{
                                        layer.msg("停止任务成功!", {icon: 1})
                                    }

                                    //layer.close(winIndex)
                                    var tmpPostData ={shield_id:data.shield_id}
                                    server_api.getTaskByShieldId(JSON.stringify(tmpPostData),function (resp) {
                                        if(resp.status==0){
                                            var updateData = resp.result[0]
                                            delete updateData.id
                                            obj.update(updateData);
                                        }
                                    })
                                    layer.close(tmpIndex)
                                } else {
                                    layer.msg(resp.msg, {icon: 2})
                                }
                            })
                        } else {
                            layer.msg("未勾选任何终端!", {icon: 0})
                        }
                    })

                }else{
                    layer.msg("没有获取到终端分组列表!",{icon:2})
                    getTerminalAndGroup()
                }
            }else{
                layer.msg("没有获取到终端列表!",{icon:2})
                getTerminalAndGroup()
            }

        }else if(event=="fail_terminals"){
            var oldTerminals = data.fail_terminals
            if(!oldTerminals||oldTerminals.length==0){
                layer.msg("没有下发失败的终端!",{icon:0})
                return
            }
            var treeDataList = []
            if (AllTerminalList != null) {
                var allTerminalList = AllTerminalList;
                var tmpIndex = popWindow("请选择终端", "#setTerminals", ["50%", "80%"])
                if (TerminalGrpList != null) {
                    var groupList = TerminalGrpList;
                    treeDataList =  common_api.buildTerminalTree(groupList,allTerminalList,oldTerminals,false)
                    tree.render({
                        id: 'terminalGrpTree'
                        , elem: '#terminalGrpTree'
                        , data: treeDataList
                        , isJump: true  //link 为参数匹配
                        , showCheckbox: true
                    });

                    $("#terminalGrpTree").append($('<div  class="align-center verical-margin">\n' +
                        '        <div id="deleteFailTerminal" class="layui-btn">删除失败记录</div>\n' +
                        '        <div id="resend2Terminal" class="layui-btn">重新下发此任务</div>\n' +
                        '    </div'))
                    $("#deleteFailTerminal").off("click");
                    $("#deleteFailTerminal").on("click", function (object) {
                        var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
                        var terminalIdList = common_api.getCheckTermialIds(checkedData)
                        if (terminalIdList.length > 0) {
                            var postData = {
                                shield_id:data.shield_id,
                                fail_terminals:terminalIdList
                            }
                            server_api.setShieldTerminals(JSON.stringify(postData), function (resp) {
                                if (resp.status == 0) {
                                    layer.msg("删除成功!", {icon: 1})
                                    layer.close(tmpIndex)
                                } else {
                                    layer.msg(resp.msg, {icon: 2})
                                }
                            })
                        } else {
                            layer.msg("未勾选任何终端!", {icon: 0})
                        }
                    })


                    $("#resend2Terminal").off("click");
                    $("#resend2Terminal").on("click", function (object) {

                        var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
                        var terminalIdList = common_api.getCheckTermialIds(checkedData)

                        if (terminalIdList.length > 0) {
                            var postData={
                                shield_id:data.shield_id,
                                terminal_type:"fail"
                            }
                            var loadingFlag = layer.msg('正在重新下发任务，请稍候……', {icon: 16, shade: 0.01, shadeClose: false, time: 0});
                            server_api.sendShielTask2Terminals(JSON.stringify(postData),function (resp) {
                                layer.close(loadingFlag)
                                if(resp.status==0){
                                    var failedCnt = resp.result[0].failedCnt
                                    if(failedCnt >0){
                                        layer.msg("发送失败终端数:"+failedCnt, {icon: 0})
                                    }else{
                                        layer.msg("保存并发送成功!", {icon: 1})
                                        var tmpPostData ={shield_id:data.shield_id}
                                        server_api.getTaskByShieldId(JSON.stringify(tmpPostData),function (resp) {
                                            if(resp.status==0){
                                                var updateData = resp.result[0]
                                                delete updateData.id
                                                obj.update(updateData);
                                            }
                                        })
                                    }
                                }else{
                                    layer.msg(resp.msg, {icon: 2})
                                }
                            })
                        } else {
                            layer.msg("未勾选任何终端!", {icon: 0})
                        }
                        //console.log(checkedData);
                    })
                }else{
                    layer.msg("没有获取到终端分组列表!",{icon:2})
                    getTerminalAndGroup()
                }
            }else{
                layer.msg("没有获取到终端列表!",{icon:2})
                getTerminalAndGroup()
            }
        }
    })

    function getTerminalAndGroup() {
        var postData = {
            page: 1,
            pagesize: 10000,
            getTotal: "yes"
        }

        server_api.getAllTerminalOfUser(JSON.stringify(postData),function (terResp) {

            if (terResp.status == 0) {
                AllTerminalList = terResp.result;
                server_api.getTerminalGrp(JSON.stringify(postData), function (resp) {
                    if (resp.status == 0) {
                        TerminalGrpList = resp.result;
                    }else{
                        layer.msg("获取终端分组失败,任务无法下发!",{icon:2})
                    }
                })
            }
        })

    }

    $(function () {
        getTerminalAndGroup()
        setCurrentData("yes")
    })

})