layui.config({
    base: '../../static/js/'
}).extend({ //设定模块别名
    admin: 'admin',
    server_api: 'server_api',
    common_api: 'common_api'
});

function getNowFormatDate(date) {
    var seperator1 = "-";
    var year = date.getFullYear();
    var month = date.getMonth() + 1;
    var strDate = date.getDate();
    if (month >= 1 && month <= 9) {
        month = "0" + month;
    }
    if (strDate >= 0 && strDate <= 9) {
        strDate = "0" + strDate;
    }
    var currentdate = year + seperator1 + month + seperator1 + strDate;
    return currentdate;
}

var pageData = {
    page: 1,
    pagesize: 10,
    lastPage: 999, //后面计算出来
    total: 0,
    dataList: null
}

var currenWinIndex =null
var TerminalGrpList =null
var AllTerminalList =null


layui.use(['jquery', 'form','tree', 'table', 'admin', 'laypage', 'server_api','common_api'], function () {
    var $ = layui.jquery,
        admin = layui.admin,
        table = layui.table,
        laypage = layui.laypage,
        form = layui.form,
        tree = layui.tree,
        common_api = layui.common_api,
        server_api = layui.server_api;

    function popWindow(title, elem, size) {
        closeCurrenWindow()
        currenWinIndex = layer.open({
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
        return currenWinIndex;
    }

    function closeCurrenWindow() {
        if(currenWinIndex != null){
            layer.close(currenWinIndex)
            currenWinIndex = null
        }
    }


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
                    //setCurrentData();
                }
            }
        });
    }


    function renderDataList(dataList) {
        table.render({
            id: "urgencyTaskList",//
            elem: '#urgencyTaskList',//指定表格元素
            data: dataList,  //表格当前页面数据
            limit: pageData.pagesize,
            toolbar: '#toolbarAction',
            defaultToolbar: ['filter'],
            cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
            skin: 'line-row', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
            cols: [[
                {checkbox: true},
                {field: 'id', align: 'left', title: '序号', width: 80, sort: true},
                {field: 'title', align: 'left', title: '预案名称(点击修改)', width: 240, sort: true,event:"modify",templet:function (val) {
                        if(val.title.length > 16){
                            return val.title.substring(0, 16) + "……"
                        }else{
                            return val.title
                        }
                    }},
                {field: 'content', align: 'left', title: '发布内容(点击修改)', width: 460, sort: true,event:"modify",templet:function (val) {
                        if(val.content.length > 16){
                            return val.content.substring(0, 16) + "……"
                        }else{
                            return val.content
                        }
                    }},
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

                {title: '操作', toolbar: '#barActions', cellMinWidth: 80},
                {field: 'creator', align: 'center', title: '创建人', width: 120},
                {field: 'create_time', align: 'center',   width: 180, title: '创建时间', templet: function (val) {
                        return val.create_time.substring(0,19)
                    }
                }
            ]]
        });

    }


    function setCurrentData(getTotal, isLast, isFirst) {
        var postData = {
            page: pageData.page,
            pagesize: pageData.pagesize,
            getTotal: getTotal
        }

        if (isLast) {
            postData.page = pageData.lastPage
        } else if (isFirst) {
            postData.page = 1
        }

        server_api.getUrgencyTaskList(JSON.stringify(postData), function (resp) {
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

    //监听行工具事件
    table.on('toolbar(urgencyTaskList)', function (obj) {
        if(obj.event=="addUrgencyTaskBtn"){
            $("#addUrgencyTask").find('[id="submitBtn"]').text("确定创建")
            $("#addUrgencyTask").find('[name="urgencyName"]').prop("value","")
            $("#addUrgencyTask").find('[name="urgencyContent"]').val("")
            popWindow("新增紧急预案","#addUrgencyTask",['800px', '400px'])
        }else if(obj.event=="delSelectUrgencyTask"){
            var data = table.checkStatus('urgencyTaskList').data;
            if (data && data.length > 0) {
                layer.confirm('确认要删除选中的紧急预案吗？操作不可恢复!', function (index) {
                    var urgency_ids = [];
                    for (var i = 0; i < data.length; i++) {
                        if(data[i].ok_terminals!=null && data[i].ok_terminals.length >0){
                            var index = i +1
                            layer.msg("第"+index +"条预案有启用的终端,不能删除!",{icon:2})
                            return
                        }else{
                            urgency_ids.push(data[i].urgency_id);
                        }
                    }
                    var postData = {urgency_ids: urgency_ids};
                    server_api.delUrgencyTask(JSON.stringify(postData), function (resp) {
                        if (resp.status == 0) {
                            //$(".layui-form-checked").not('.header').parents('tr').remove();
                            layer.msg('删除成功', {
                                icon: 1
                            });
                            addOrDelItem("del", urgency_ids.length);
                            setCurrentData();
                        } else {
                            layer.msg('删除失败', {
                                icon: 2
                            });
                        }
                    });
                });
            } else {
                layer.msg('请选择需要删除的紧急预案！', {
                    icon: 0
                });
            }

        }

    })


    //监听行工具事件
    table.on('tool(urgencyTaskList)', function (obj) {
        var data = obj.data;
        if(obj.event=="modify"){
            if(data.ok_terminals.length >0){
                layer.msg("预案有启用的终端，不可以修改!",{icon:0})
                return
            }

            $("#addUrgencyTask").find('[name="urgencyName"]').prop("value",data.title)
            $("#addUrgencyTask").find('[name="urgencyContent"]').val(data.content)
            $("#addUrgencyTask").find('[id="submitBtn"]').text("提交修改")
            $("#addUrgencyTask").find('[id="urgency_id"]').attr("name",data.urgency_id)

            popWindow("修改预案内容","#addUrgencyTask",['40%', '50%'])
        } else if (obj.event === 'del') {//单项删除
            if(data.ok_terminals.length >0){
                layer.msg("预案有启用的终端，不可以删除!",{icon:0})
                return
            }
            layer.confirm('是否删除此紧急预案信息！', function (index) {
                var urgency_ids = [];
                urgency_ids.push(data.urgency_id);
                var postData = {urgency_ids: urgency_ids};
                server_api.delUrgencyTask(JSON.stringify(postData), function (resp) {
                    if (resp.status == 0) {
                        obj.del();
                        layer.msg("删除成功!",{icon:1})
                        addOrDelItem("del", urgency_ids.length);
                        setCurrentData("yes");
                    }else{
                        layer.msg("删除失败!",{icon:2})
                    }
                });

                layer.close(index);
            });
        }else if(obj.event=="ready_terminals"){
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
                                urgency_id:data.urgency_id,
                                ready_terminals:terminalIdList
                            }
                            server_api.setUrgencyTerminal(JSON.stringify(postData), function (resp) {
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
                            var postData2={
                                urgency_ids:[data.urgency_id],
                                terminal_ids:terminalIdList
                            }
                            var loadingFlag = layer.msg('正在下发任务，请稍候……', {icon: 16, shade: 0.01, shadeClose: false, time: 0});
                            server_api.startUrgencyTask(JSON.stringify(postData2),function (resp) {
                                layer.close(loadingFlag)
                                if(resp.status==0){
                                    var failedCnt = resp.result[0].failedCnt
                                    if(failedCnt >0){
                                        layer.msg("发送失败终端数:"+failedCnt, {icon: 0})
                                    }else{
                                        layer.msg("保存并发送成功!", {icon: 1})
                                    }
                                    var tmpPostData ={urgency_id:data.urgency_id}
                                    server_api.getUrgencyTaskById(JSON.stringify(tmpPostData),function (resp) {
                                        if(resp.status==0){
                                            var updateData = resp.result[0]
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
        }else if(obj.event=="ok_terminals"){
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
                        '        <div id="deleteTaskFromTerminals" class="layui-btn">在勾选的终端上删除此任务</div>\n' +
                        '</div>'))
                    $("#deleteTaskFromTerminals").off("click");
                    $("#deleteTaskFromTerminals").on("click", function (object) {
                        var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
                        var terminalIdList = common_api.getCheckTermialIds(checkedData)
                        if (terminalIdList.length > 0) {
                            var postData = {
                                urgency_id:data.urgency_id,
                                terminal_ids:terminalIdList
                            }
                            var loadingFlag = layer.msg('正在停止任务，请稍候……', {icon: 16, shade: 0.01, shadeClose: false, time: 0});
                            server_api.stopUrgencyTask(JSON.stringify(postData), function (resp) {
                                layer.close(loadingFlag)
                                if (resp.status == 0) {
                                    layer.msg("停止任务成功!", {icon: 1})
                                    //layer.close(winIndex)
                                    var tmpPostData ={urgency_id:data.urgency_id}
                                    server_api.getUrgencyTaskById(JSON.stringify(tmpPostData),function (resp) {
                                        if(resp.status==0){
                                            var updateData = resp.result[0]
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

        }else if(obj.event=="fail_terminals") {
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
                    treeDataList = common_api.buildTerminalTree(groupList, allTerminalList, oldTerminals, false)
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
                                urgency_id: data.urgency_id,
                                fail_terminals: terminalIdList
                            }
                            server_api.setUrgencyTerminal(JSON.stringify(postData), function (resp) {
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
                            var postData = {
                                urgency_ids: [data.urgency_id],
                                terminal_type: "fail"
                            }
                            var loadingFlag = layer.msg('正在重新下发任务，请稍候……', {icon: 16, shade: 0.01, shadeClose: false, time: 0});
                            server_api.startUrgencyTask(JSON.stringify(postData), function (resp) {
                                layer.close(loadingFlag)
                                if (resp.status == 0) {
                                    var failedCnt = resp.result[0].failedCnt
                                    if (failedCnt > 0) {
                                        layer.msg("发送失败终端数:" + failedCnt, {icon: 0})
                                    } else {
                                        layer.msg("保存并发送成功!", {icon: 1})
                                        var tmpPostData = {urgency_id: data.urgency_id}
                                        server_api.getUrgencyTaskById(JSON.stringify(tmpPostData), function (resp) {
                                            if (resp.status == 0) {
                                                var updateData = resp.result[0]
                                                obj.update(updateData);
                                            }
                                        })
                                    }
                                } else {
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
            } else {
                layer.msg("没有获取到终端列表!", {icon: 2})
                getTerminalAndGroup()
            }
        }
    });


    //监听提交新的紧急预案
    form.on('submit(submitNewTask)', function (data) {
        var newTask = {}
        var now = new Date();
        var successMsg = "新增成功!"
        var isAdd = true

        var btnText = $("#addUrgencyTask").find('[id="submitBtn"]').text()
        if(btnText=="提交修改"){
           var urgency_id =  $("#addUrgencyTask").find('[id="urgency_id"]').attr("name")
            newTask.urgency_id = urgency_id
            newTask.status = 0
            successMsg = "修改成功"
            isAdd =false
        }

        newTask.title = data.field["urgencyName"]
        newTask.content = data.field["urgencyContent"]
        newTask.status = status
        server_api.urgencyAddOrUpdate(JSON.stringify(newTask), function (resp) {
            if (resp.status == 0) {
                layer.msg(successMsg, {icon: 1});
                if(isAdd){
                    addOrDelItem("add", 1);
                    closeCurrenWindow()
                    setCurrentData("yes");
                }else{
                    setCurrentData();
                }
                //跳转到最后一页
            } else {
                layer.msg(resp.msg, {icon: 2});
            }
        });
        return false;

    });


    //监听单元格编辑
    table.on('edit(urgencyTaskList)', function (obj) {
        var value = obj.value //得到修改后的值
            , data = obj.data //得到所在行所有键值
            , field = obj.field; //得到字段

        if (data.title == null || data.title.length == 0 || data.content == null || data.content.length == 0) {
            setCurrentData("no");
            layer.msg("不能修改为空数据!", {icon: 2});
        } else {
            var postData = {
                urgency_id: data.urgency_id,
                title: data.title,
                content: data.content,
                status:data.status,
                terminal_ids:data.terminal_ids
            }
            server_api.urgencyAddOrUpdate(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    layer.msg("修改成功!", {icon: 1});
                } else {
                    setCurrentData("no");
                    layer.msg("修改失败!", {icon: 2});
                }
            });
        }

    });


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
        setCurrentData("yes");
    })

});