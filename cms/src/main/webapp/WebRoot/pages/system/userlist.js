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

var userGroupData ={
    page: 1,
    pagesize: 10,
    lastPage: 999, //后面计算出来
    total: 0,
    dataList: null
}

var currentUser = null;


layui.use(['jquery', 'form', 'tree', 'table', 'admin', 'laypage', 'laydate','transfer', 'server_api'], function () {
    var $ = layui.jquery,
        admin = layui.admin,
        table = layui.table,
        laypage = layui.laypage,
        laydate = layui.laydate,
        form = layui.form,
        tree = layui.tree,
        transfer =layui.transfer,
        server_api = layui.server_api;



    function showCurrentUser() {
        server_api.getUserInfo(null, function (resp) {
            if (resp.status == 0) {
                currentUser = resp.result;
                if(currentUser[0].is_supper==1){
                    $("#allUserList").show()
                }
                var currentUsrCols = [
                    {field: 'username', align: 'left', title: '用户名', width: 200},
                    {
                        field: 'password',
                        align: 'center',
                        title: '登录密码',
                        width: 100,
                        event: "password",
                        templet: function (val) {
                            return '<span class="layui-btn layui-btn-xs">修改</span>';

                        }
                    },
                    {
                        field: 'identity_num',
                        align: 'left',
                        title: '身份证号码',
                        width: 180,
                        edit: "text",
                        event: "identity_num"
                    },
                    {
                        field: 'phone_num',
                        align: 'left',
                        title: '手机号码',
                        width: 120,
                        edit: "text",
                        event: "phone_num"
                    },
                    {field: 'qq', align: 'left', title: 'QQ号码', width: 140, edit: "text", event: "qq"},
                    {field: 'weixin', align: 'left', title: '微信号', width: 140, edit: "text", event: "weixin"},
                    {field: 'mail', align: 'left', title: '邮箱', width: 200, edit: "text", event: "mail"},
                    {
                        field: 'real_name',
                        align: 'left',
                        title: '真实姓名',
                        width: 200,
                        edit: "text",
                        event: "real_name"
                    },
                    {
                        field: 'is_supper',
                        align: 'left',
                        title: '是否超级用户',
                        cellMinWidth: 120,
                        event: "is_supper",
                        templet: function (val) {
                            if (val.is_supper == 1) {
                                return "是"
                            } else {
                                return "否"
                            }
                        }
                    }
                ]
                table.render({
                    id: "currentUser",//
                    elem: '#currentUser',//指定表格元素
                    data: currentUser,  //表格当前页面数据
                    limit: pageData.pagesize,
                    toolbar: "#currentUserToolbar",
                    defaultToolbar: [],
                    cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
                    skin: 'line ', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
                    done: function (res, curr, count) {

                    },
                    cols: [currentUsrCols]
                })

                setCurrentData("yes", "all")
            } else {
                layer.msg("获取用户信息失败!")

            }
        })


    }

    //监听单元格编辑
    table.on('edit(currentUser)', function (obj) {
        var value = obj.value //得到修改后的值
            , data = obj.data //得到所在行所有键值
            , field = obj.field; //得到字段

        if (!value || value.length == 0) {
            layer.msg("不能修改为空!", {icon: 2})
            showCurrentUser()
            return
        } else {
            var postData = data
            postData[field] = value
            server_api.userModifyInfo(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    layer.msg("修改成功!", {icon: 1})
                } else {
                    layer.msg("修改失败!", {icon: 2})
                }
            })
        }
    })

    //监听任务表单项修改
    table.on('tool(currentUser)', function (obj) {
        var data = obj.data;
        var event = obj.event;
        if (event == "password") {
            var tmpWinIndex = popWindow("修改密码", "#modifyPwd", ["300px", "260px"])
            //监听修改密码
            form.on('submit(submitNewpwd)', function (data) {
                var field = data.field
                if (field.password != field.confirm_pwd) {
                    layer.msg("新密码不一致,请重新输入!", {icon: 2})
                    return
                } else {
                    server_api.userModifyPwd(JSON.stringify(field), function (resp) {
                        if (resp.status == 0) {
                            layer.msg("修改成功,请重新登录！", {icon: 1})
                        } else {
                            layer.msg(resp.msg, {icon: 2})
                        }
                    })
                }
                layer.close(tmpWinIndex)
                return false;
            });
        }
    })


    //用户分组管理
    function renderUserGroupPages(total) {
        laypage.render({
            elem: 'group-pages'
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

    function refreshUserGroupData(getTotal) {
        var postData = {
            page: userGroupData.page,
            pagesize: userGroupData.pagesize,
            getTotal: getTotal
        }

        server_api.getUserGroupList(JSON.stringify(postData), function (resp) {
            var dataList = [];
            if (resp.status == 0) {
                dataList = resp.result;
                userGroupData.dataList = dataList;
                if (getTotal == "yes") {
                    renderUserGroupPages(resp.total);
                }
            } else  {
                renderUserGroupPages(0);
                layer.msg(resp.msg, {icon: 2});
            }
            renderUserGrpDataList(dataList)
        });
    }

    function renderUserGrpDataList(dataList) {
        var colsList =[]
        colsList.push({checkbox: true})
        colsList.push({type:"numbers", align: 'left', title: '序号', width: 80})
        if(currentUser[0].is_supper==1){
            colsList.push({title:'操作',align: 'left', width:80,templet:function (val) {
                    var delBtn = '<div class="layui-btn  layui-btn-xs layui-btn-danger" lay-event="delUserGroup">删除</div>'
                    return delBtn
                }})
        }

        if(currentUser[0].is_supper==1){
            colsList.push({field: 'group_name', align: 'left', title: '用户分组名称(可编辑)',edit:"text", width: "40%", event: "group_name"})
        }else{
            colsList.push({field: 'group_name', align: 'left', title: '用户分组名称(可编辑)', width: "40%", event: "group_name"})
        }


        colsList.push({field: 'user_members', align: 'left', title: '组内用户列表', width: "15%", event: "user_members",templet:function (val) {
                return '<span class="layui-btn layui-btn-xs">查看列表</span>';

            }})

        colsList.push({field: 'terminal_groups', align: 'left', title: '授权管理的终端分组', width: "15%", event: "terminal_groups",templet:function (val) {
                return '<span class="layui-btn layui-btn-xs">查看列表</span>';
            }})
        colsList.push(  {field: 'create_time', align: 'left', title: '创建时间',templet:function (val) {
                return val.create_time.substring(0,19)
            }})


        var renderObj ={
            id: "userGroupList",//
            elem: '#userGroupList',//指定表格元素
            data: dataList,  //表格当前页面数据
            // height: 'full-20',
            limit: pageData.pagesize,
            defaultToolbar: ['filter'],
            cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
            skin: 'line ', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
            done: function (res, curr, count) {
                $(".layui-table-main  tr").each(function (index, val) {
                    $($(".layui-table-fixed .layui-table-body tbody tr")[index]).height($(val).height());
                });
            },
            // parseData: function (res) { //res 即为原始返回的数据
            //     console.log("!!!!!!!!!!!!!!!!!!!run into rable parse data");
            //     console.log(res)
            //     return res
            // },
            cols: [colsList]
        }
        if(currentUser[0].is_supper==1){
            renderObj.toolbar = "#userGroupToolbar"
        }else{
            renderObj.toolbar = "#userGroupToolbar2"
        }

        table.render(renderObj)
    }

    //监听单元格编辑
    table.on('edit(userGroupList)', function (obj) {
        var value = obj.value //得到修改后的值
            , data = obj.data //得到所在行所有键值
            , field = obj.field; //得到字段

        if (!value || value.length == 0) {
            layer.msg("不能修改为空!", {icon: 2})
            return
        } else {
            var postData = {}
            postData["group_id"] =data["group_id"]
            postData[field] = value
            server_api.addOrUpdateUserGroup(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    var newdata ={}
                    newdata[field] = value;
                    obj.update(newdata);
                    layer.msg("修改成功!", {icon: 1})
                } else {
                    layer.msg("修改失败!", {icon: 2})
                }
            })
        }
    })

    //监听任务表单项修改
    table.on('tool(userGroupList)', function (obj) {
        var data = obj.data;
        var event = obj.event;
        if(event=="delUserGroup"){
            layer.confirm('删除用户分组不会删除用户,是否确定删除?', {icon: 3, title:'提示'}, function(index){
                var postData={
                    group_ids:[data.group_id]
                }
                server_api.delUserGroups(JSON.stringify(postData),function (resp) {
                    if(resp.status==0){
                        //obj.del();
                        layer.msg("删除成功!",{icon:1})
                        refreshUserGroupData("yes")
                    }else{
                        layer.msg(resp.msg,{icon:2})
                    }
                })
            });

        }else if (event == "user_members") {

            var tmpWinIndex = popWindow("组内成员管理", "#modifyUserMembers", ["60%", "60%"])
            if(currentUser[0].is_supper==0){
                $("#submitMembers").remove()
            }
            var userMebers = data.user_members
            var postData ={
                page: 1,
                pagesize: 999,
                getTotal: "yes"
            }
            server_api.getUserList(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    //dataList = resp.result;
                    var groupMemberList =[];
                    var tmpUserMembers = userMebers;
                    if(tmpUserMembers && tmpUserMembers.length >0){
                        groupMemberList = tmpUserMembers.split(";")
                    }
                    var allUserList =[];
                    var showMemberList =[];

                    for(var i=0;i<resp.result.length;i++){
                        var userItem ={}
                        userItem.value = resp.result[i].uid;
                        userItem.title = resp.result[i].username;
                        if(currentUser[0].is_supper==0){
                            userItem.disabled = true;
                        }
                        for(var j=0;j<groupMemberList.length;j++){
                            if(groupMemberList[j]==userItem.value){
                                showMemberList.push(userItem.value)
                                break
                            }
                        }
                        if(resp.result[i].is_supper==0){
                            allUserList.push(userItem);
                        }
                    }
                    if(currentUser[0].is_supper==0){
                        $("#modifyUserMembers").find("legend").text("查看组内用户")
                    }else{
                        $("#modifyUserMembers").find("legend").text("请修改组内用户")
                    }


                    transfer.render({
                        id:"membersTransfer"
                        ,elem: "#membersTransfer"
                        ,data: allUserList
                        ,title: ["其他用户","组内用户"]
                        ,showSearch: true
                        ,width:'45%'
                        ,height:300
                        ,value: showMemberList
                        ,onchange: function(obj, index){

                        }
                    })

                    $("#modifyUserMembers").find('[id=submitMembers]').off("click");
                    $("#modifyUserMembers").find('[id=submitMembers]').on("click",function (object) {
                        var getData = transfer.getData("membersTransfer");
                        var newMemberList =[]
                        if(getData && getData.length >0){
                            for(var i=0;i<getData.length;i++){
                                newMemberList.push(getData[i].value)
                            }
                        }
                        var postData2 ={
                            group_id:data.group_id,
                            group_name:data.group_name,
                            user_members:newMemberList
                        }

                        layer.close(tmpWinIndex);
                        server_api.addOrUpdateUserGroup(JSON.stringify(postData2), function (resp) {
                            if(resp.status==0){
                                var newdata ={}
                                newdata.user_members = newMemberList.join(";");
                                obj.update(newdata);
                                layer.msg("保存分组用户成功!",{icon:1})
                            }else{
                                layer.msg("保存分组用户失败!",{icon:2})
                            }
                        });
                    })

                }
            });

        }else if(event=="terminal_groups"){
            var tmpWinIndex = popWindow("授权可操作终端分组", "#modifyTerminalGroups", ["60%", "60%"])
            if(currentUser[0].is_supper==0){
                $("#submitTerminalGroups").remove()
            }

            var terminalGroups = data.terminal_groups
            var postData ={
                page: 1,
                pagesize: 999,
                getTotal: "yes"
            }
            server_api.getTerminalGrp(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    var terminalGroupList =[];
                    var tmpTerminalGroups = terminalGroups;
                    if(tmpTerminalGroups && tmpTerminalGroups.length >0){
                        terminalGroupList = tmpTerminalGroups.split(";")
                    }
                    var allTerminalGroupList =[];
                    var showTerminalGroupList =[];

                    for(var i=0;i<resp.result.length;i++){
                        var terminalGrpItem ={}
                        terminalGrpItem.value = resp.result[i].gid;
                        terminalGrpItem.title = resp.result[i].group_name;
                        if(currentUser[0].is_supper==0){
                            terminalGrpItem.disabled = true;
                        }
                        for(var j=0;j<terminalGroupList.length;j++){
                            if(terminalGroupList[j]==terminalGrpItem.value){
                                showTerminalGroupList.push(terminalGrpItem.value)
                                break
                            }
                        }
                        allTerminalGroupList.push(terminalGrpItem);
                    }

                    if(currentUser[0].is_supper==0){
                        $("#modifyTerminalGroups").find("legend").text("查看该组用户可以管理的终端分组")
                    }else{
                        $("#modifyTerminalGroups").find("legend").text("请修改该组用户可以管理的终端分组")
                    }

                    transfer.render({
                        id:"terminalGrpTransfer"
                        ,elem: "#terminalGrpTransfer"
                        ,data: allTerminalGroupList
                        ,title: ["未授权的终端分组","已经授权的终端分组"]
                        ,showSearch: true
                        ,width:'45%'
                        ,height:300
                        ,value: showTerminalGroupList
                        ,onchange: function(obj, index){

                        }
                    })

                    $("#modifyTerminalGroups").find('[id=submitTerminalGroups]').off("click");
                    $("#modifyTerminalGroups").find('[id=submitTerminalGroups]').on("click",function (object) {
                        var getData = transfer.getData("terminalGrpTransfer");
                        var newTerminalGrpList =[]
                        if(getData && getData.length >0){
                            for(var i=0;i<getData.length;i++){
                                newTerminalGrpList.push(getData[i].value)
                            }
                        }
                        var postData2 ={
                            group_id:data.group_id,
                            group_name:data.group_name,
                            terminal_groups:newTerminalGrpList
                        }

                        layer.close(tmpWinIndex);
                        server_api.addOrUpdateUserGroup(JSON.stringify(postData2), function (resp) {
                            if(resp.status==0){
                                var newdata ={}
                                newdata.terminal_groups = newTerminalGrpList.join(";");
                                obj.update(newdata);
                                layer.msg("保存授权终端分组成功!",{icon:1})
                            }else{
                                layer.msg("保存授权终端分组失败!",{icon:2})
                            }
                        });
                    })

                }else{
                    layer.msg("当前没有终端分组,请先分组!",{icon:2})
                }
            });
        }
    })


    table.on('toolbar(userGroupList)', function (obj) {

        if (obj.event == "addNewGroup") {
            var tmpWinIndex = popWindow("新增用户分组", "#addUserGroup", ["500px", "200px"]);
            //监听提交新的用户
            form.on('submit(submitNewUserGroup)', function (data) {
                var field = data.field
                if (field.group_name.length==0) {
                    layer.msg("分组名称不能为空!", {icon: 2})
                    return
                } else {
                    server_api.addOrUpdateUserGroup(JSON.stringify(field), function (resp) {
                        if (resp.status == 0) {
                            layer.msg("新增成功!", {icon: 1})
                            refreshUserGroupData("yes")
                        } else {
                            layer.msg(resp.msg, {icon: 2})
                        }
                    })
                }
                layer.close(tmpWinIndex)
                return false;

            });


        } else if (obj.event == "batchDelUserGroup") {
            var data = table.checkStatus('userGroupList').data;
            if (data.length == 0) {
                layer.msg("未勾选任何用户！", {icon: 0})
                return
            } else {
                layer.confirm('真的删除勾选的用户分组么?', {icon: 3, title: '提示'}, function (index) {
                    var groupids = []
                    for (var i = 0; i < data.length; i++) {
                        groupids.push(data[i].group_id)
                    }
                    var postData = {
                        group_ids: groupids
                    }
                    server_api.delUserGroups(JSON.stringify(postData), function (resp) {
                        if (resp.status == 0) {
                            layer.msg("删除成功!", {icon: 1})
                            refreshUserGroupData("yes")
                        } else {
                            layer.msg(resp.msg, {icon: 2})
                        }
                    })
                })
            }

        }
    })


    /////////////////////////////////////////////////////////////////////


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

        server_api.getUserList(JSON.stringify(postData), function (resp) {
            var dataList = [];
            if (resp.status == 0) {
                //refreshCurrentPage(table,refreshResp.result);
                dataList = resp.result;
                pageData.dataList = dataList;
                if (getTotal == "yes") {
                    renderPages(resp.total);
                }
            } else {
                renderPages(0);
                layer.msg(resp.msg, {icon: 2});
            }
            if (currentUser != null && currentUser[0].is_supper == 1) {
                renderDataList(dataList);
            }

        });
    }

    function renderDataList(dataList) {
        var colsList = [
            {checkbox: true},
            {field: 'id', align: 'left', title: '序号', width: 80, sort: true},
            {field: 'username', align: 'left', title: '用户名', width: 120, sort: true, event: "username"},
            // {
            //     field: 'password',
            //     align: 'center',
            //     title: '登录密码',
            //     width: 100,
            //     event: "password",
            //     templet: function (val) {
            //         return '<span class="layui-btn layui-btn-xs">重置</span>';
            //     }
            // },
            {field: 'identity_num', align: 'left', title: '身份证号码', width: 180, sort: true, event: "identity_num"},
            {field: 'phone_num', align: 'left', title: '手机号码', width: 120, sort: true, event: "phone_num"},
            {field: 'qq', align: 'left', title: 'QQ号码', width: 140, sort: true, event: "qq"},
            {field: 'weixin', align: 'left', title: '微信号', width: 140, sort: true, event: "weixin"},
            {field: 'mail', align: 'left', title: '邮箱', width: 160, event: "mail"},
            {field: 'real_name', align: 'left', title: '真实姓名', width: 100, event: "real_name"},
            {
                field: 'is_supper',
                align: 'center',
                title: '是否超级用户',
                width: 120,
                event: "is_supper",
                templet: function (val) {
                    if(val.is_supper==0){
                        return "<span class='layui-btn layui-btn-sm layui-btn-danger'>否</span>"
                    }else{
                        return "<span class='layui-btn layui-btn-sm layui-btn-normal'>是</span>"
                    }

                }
            },
            {field: 'create_time', align: 'left', title: '注册时间',templet:function (val) {
                    return val.create_time.substring(0,19)
                }},
            // {field: 'update_time', align: 'left', title: '最后修改时间'}
        ]

        if (currentUser != null && currentUser[0].is_supper != 1) {
            colsList.splice(0, 1)
        }

        var renderObj ={
            id: "userList",//
            elem: '#userList',//指定表格元素
            data: dataList,  //表格当前页面数据
            // height: 'full-20',
            limit: pageData.pagesize,
            defaultToolbar: ['filter'],
            cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
            skin: 'line ', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
            done: function (res, curr, count) {
                // for(var i =0;i<res.data.length;i++){
                //     if(res.data[i].task_name =="新的任务名字"){
                //         $("tr").eq(i+1).css("background-color","rgba(255,88,39,0.4)").css("color","white")
                //     }
                // }
                $(".layui-table-main  tr").each(function (index, val) {
                    $($(".layui-table-fixed .layui-table-body tbody tr")[index]).height($(val).height());
                });
            },
            // parseData: function (res) { //res 即为原始返回的数据
            //     console.log("!!!!!!!!!!!!!!!!!!!run into rable parse data");
            //     console.log(res)
            //     return res
            // },
            cols: [colsList]
        }

        if (currentUser != null && currentUser[0].is_supper == 1) {
            renderObj.toolbar = '#toolbarAction'
        }


        table.render(renderObj)
    }

    form.verify({
        //数组的两个值分别代表：[正则匹配、匹配不符时的提示文字]
        username: [
            /^[a-zA-Z][a-zA-Z0-9]*$/
            , '用户名只能输入英文字母和数字'
        ],
        userpwd: [
            // /(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[^a-zA-Z0-9]).{8,30}/
            /.{4,30}/
            // , "密码必须包含大小字母、数字、特殊字符，至少8位，最多30位"
            , "密码至少4位，最多30位"
        ]
    });


    table.on('toolbar(userList)', function (obj) {

        if (obj.event == "addNewUser") {
            var tmpWinIndex = popWindow("新增管理用户", "#addUser", ["20%", "40%"]);
            //监听提交新的用户
            form.on('submit(submitNewUser)', function (data) {
                var field = data.field
                if (field.password != field.confirm_pwd) {
                    layer.msg("密码不一致,请重新输入!", {icon: 2})
                    return
                } else {
                    server_api.addNewUser(JSON.stringify(field), function (resp) {
                        if (resp.status == 0) {
                            layer.msg("添加成功!", {icon: 1})
                            addOrDelItem("add", 1)
                            setCurrentData("yes")
                        } else {
                            layer.msg(resp.msg, {icon: 2})
                        }
                    })
                }
                layer.close(tmpWinIndex)
                return false;

            });


        } else if (obj.event == "delSelect") {
            var data = table.checkStatus('userList').data;
            if (data.length == 0) {
                layer.msg("未勾选任何用户！", {icon: 0})
                return
            } else {
                layer.confirm('真的删除勾选的用户么?', {icon: 3, title: '提示'}, function (index) {
                    var user_ids = []
                    for (var i = 0; i < data.length; i++) {
                        user_ids.push(data[i].uid)
                    }
                    var postData = {
                        user_ids: user_ids
                    }
                    server_api.delUser(JSON.stringify(postData), function (resp) {
                        if (resp.status == 0) {
                            layer.msg("删除成功!", {icon: 1})
                            addOrDelItem("del", user_ids.length)
                            setCurrentData("yes");
                        } else {
                            layer.msg(resp.msg, {icon: 2})
                        }
                    })
                })

            }

        }
    })

    //监听任务表单项修改
    table.on('tool(userList)', function (obj) {
        // console.log("run into click table!!");
        // var data = obj.data;
        // var event = obj.event;



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
        showCurrentUser()
        refreshUserGroupData("yes")
    })


})