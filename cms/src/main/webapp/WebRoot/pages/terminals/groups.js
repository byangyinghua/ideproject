layui.config({
    base: '../../static/js/'
}).extend({ //设定模块别名
    admin: 'admin',
    server_api: 'server_api'
});

var pageData = {
    page: 1,
    pagesize: 10,
    lastPage: 999, //后面计算出来
    total: 0,
    dataList: null
}

var groupTypeList={
    "normal":"普通终端",
    "loudspeaker":"外接音箱",
    "led":"外接LED大屏"
}

var currentUser =null
layui.use(['jquery', 'form', 'table', 'admin', 'laypage', 'server_api'], function () {
    var $ = layui.jquery,
        admin = layui.admin,
        table = layui.table,
        laypage = layui.laypage,
        form = layui.form,
        server_api = layui.server_api;
      var verifyMap = {
          //数组的两个值分别代表：[正则匹配、匹配不符时的提示文字]
          group_code: [/^[a-zA-Z][a-zA-Z0-9]*$/,'分组编号只能包含字母和数字,以字母开头!']
      }

      form.verify(verifyMap);

    function renderPages(total) {
        laypage.render({
            elem: 'pages'
            ,count: total
            ,layout: ['count', 'prev', 'page', 'next', 'limit', 'refresh', 'skip']
            ,jump: function(obj){
                pageData.total = total;
                pageData.page = obj.curr;
                pageData.pagesize = obj.limit;
                var m = total/pageData.pagesize;
                var n = total%pageData.pagesize;
                pageData.lastPage = n > 0?m + 1:n;

                if(total >0){
                    setCurrentData();
                }
            }
        });
    }


    function renderDataList(dataList) {
        server_api.getUserInfo(null, function (resp) {
            if (resp.status == 0) {
                currentUser = resp.result[0]
                var colsList =[]
                var cols =[]
                cols.push({checkbox: true})
                cols.push({type:"numbers", title: '序号', width: 80})
                if(currentUser.is_supper==1){
                    cols.push({title: '操作', toolbar: '#cellAction', width: 80})
                    cols.push({field: 'group_name', title: '分组名称(可编辑)',width: 400,align:"left",edit: 'text'})
                    // cols.push({field: 'group_code', title: '分组编号(可编辑)',width: 200,align:"left", edit: 'text'})
                }else{
                    cols.push({field: 'group_name', title: '分组名称(可编辑)',width: 400,align:"left"})
                    // cols.push({field: 'group_code', title: '分组编号(可编辑)',width: 200,align:"left"})
                }

                cols.push({field: 'group_type', title: '分组类型', width: 200,align:"center",templet:function (val) {
                        if(!val.group_type||val.group_type.length==0){
                            return "普通终端"
                        }else{
                            return val.group_type
                        }

                    }})

                cols.push({field:'terminal_cnt',title: '组内设备数',width:120, align:"center",templet:function (val) {
                        if(val.terminal_cnt==0){
                            return "<span class='layui-btn layui-btn-sm layui-btn-danger'>0</span>"
                        }else{
                            return "<span class='layui-btn layui-btn-sm layui-btn-primary'>" +val.terminal_cnt + "</span>"
                        }
                    }})
                cols.push({field:'onlineCnt',title: '在线设备数',width:120, align:"center",templet:function (val) {
                        if(val.onlineCnt==0){
                            return "<span class='layui-btn layui-btn-sm layui-btn-danger'>0</span>"
                        }else{
                            return "<span class='layui-btn layui-btn-sm layui-btn-checked'>" +val.onlineCnt + "</span>"
                        }

                    }})
                cols.push({field: 'update_time', title: '分组创建时间',align:"center"})
                colsList.push(cols)
                    var renderOpt ={
                        id: "groupList",//
                        elem: '#groupList',//指定表格元素
                        data: dataList,  //表格当前页面数据
                        defaultToolbar: ['filter'],
                        limit: pageData.pagesize,
                        cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
                        skin: 'line-row', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格） line-row 行列边框
                        cols: colsList
                    }

                if (currentUser.is_supper == 1) {
                    renderOpt.toolbar="#actionToolbar"
                }else{
                    renderOpt.toolbar="#actionToolbar2"
                }
                table.render(renderOpt);
            }
        })

    }


    function  setCurrentData(getTotal,isLast,isFirst,gid) {
        var postData ={
            page:pageData.page,
            pagesize:pageData.pagesize,
            getTotal:getTotal
        }

        if(isLast && isLast =="yes"){
            postData.page =  pageData.lastPage;
        }else if(isFirst && isFirst=="yes"){
            postData.page = 1;
        }

        if(gid && gid.length >0){
            postData.gid = gid;
        }

        server_api.getTerminalGrp(JSON.stringify(postData),function (resp) {
            var dataList = [];
            if(resp.status==0){
                //refreshCurrentPage(table,refreshResp.result);
                dataList = resp.result;
                pageData.dataList = dataList;
                if(getTotal == "yes"){
                    renderPages(resp.total);
                }
            }else{
                renderPages(0);
                layer.msg(resp.msg,{icon:2});
            }
            renderDataList(dataList);
        });
    }


    table.on('toolbar(groupList)',function (obj) {
        var event = obj.event
        var data = obj.data

        if(event=="delSelectGrp"){
            var data = table.checkStatus('groupList').data;
            if (data && data.length > 0) {
                layer.confirm('确认要删除选中的分组吗？操作不可恢复!', function (index) {
                    var gids = [];
                    for (var i = 0; i < data.length; i++) {
                        gids.push(data[i].gid);
                    }
                    var postData = {gids: gids};
                    server_api.delTerminalGroup(JSON.stringify(postData), function (resp) {
                        if (resp.status == 0) {
                            //$(".layui-form-checked").not('.header').parents('tr').remove();
                            layer.msg('删除成功', {
                                icon: 1
                            });
                            setCurrentData("yes");
                        } else {
                            layer.msg('删除失败', {
                                icon: 2
                            });
                        }
                    });
                });
            } else {
                layer.msg('请选择需要删除的分组', {
                    icon: 0
                });
            }
        }else if(event =="action-addgroup"){
           var popIndex =  layer.open({
                type: 1,
                shade: 0.1,
                shadeClose: true,
                title: "添加分组", //不显示标题
                closeBtn: 1,
                resize: false,
                area: ['380px', '240px'],
                content: $('#add-group'), //捕获的元素，注意：最好该指定的元素要存放在body最外层，否则可能被其它的相对元素所影响
                yes: function (index, layero) {
                    //do something
                },
                cancel: function () {

                }
            });

            $("#theNewGroupName").val("")
            $("#theNewGroupCode").val("")

            //监听提交新的分组
            form.on('submit(submit-newgrp)', function (data) {
                server_api.addOrUpdateGroup(JSON.stringify(data.field), function (resp) {
                    if (resp.status == 0) {
                        layer.msg("新增分组成功!", {icon: 1});
                        setCurrentData("yes");
                        layer.close(popIndex)
                        //跳转到最后一页
                    } else {
                        layer.msg(resp.msg, {icon: 2});
                    }
                });
                return false;
            });
        }

    })


    //监听行工具事件
    table.on('tool(groupList)', function (obj) {
        var data = obj.data;
        if (obj.event === 'del') {//单项删除
            layer.confirm('删除此分组，不会删除组内的终端！', function (index) {
                var gids = [];
                gids.push(data.gid);
                var postData = {gids: gids};

                server_api.delTerminalGroup(JSON.stringify(postData), function (resp) {
                    if (resp.status == 0) {
                        obj.del();
                        setCurrentData("yes");
                    }
                });
                layer.close(index);
            });
        }
    });


    //监听单元格编辑
    table.on('edit(groupList)', function (obj) {
        var value = obj.value //得到修改后的值
            , data = obj.data //得到所在行所有键值
            , field = obj.field; //得到字段

        if (data.group_name == null || data.group_name.length == 0 || data.group_code == null || data.group_code.length == 0) {
            setCurrentData("no");
            layer.msg("不能修改为空!", {icon: 2});
        } else {
            var postData = {
                gid: data.gid,
                group_code: data.group_code,
                group_name: data.group_name
            }
            var testReg = verifyMap["group_code"][0]
            if(testReg.test(data.group_code)==false){
                layer.msg(verifyMap["group_code"][1],{icon:0})
                $(".layui-laypage-btn").click();
                return
            }

            server_api.addOrUpdateGroup(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    layer.msg("修改成功!", {icon: 1});
                } else {
                   // setCurrentData("no");
                    layer.msg("修改失败!", {icon: 2});
                }
                $(".layui-laypage-btn").click();
            });
        }

    });


    $(function () {
        setCurrentData("yes");
    });
});