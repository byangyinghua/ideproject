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

var defaultServer = {
    server_ip:"192.168.101.0",
    server_type:"nvr",
    server_name:"请修改",
    login_name:"请修改",
    login_pwd:"123456"
};


layui.use(['jquery', 'form', 'tree', 'table', 'admin', 'laypage', 'laydate', 'server_api'], function () {
    var $ = layui.jquery,
        admin = layui.admin,
        table = layui.table,
        laypage = layui.laypage,
        laydate = layui.laydate,
        form = layui.form,
        tree = layui.tree,
        server_api = layui.server_api;


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

    function setCurrentData(getTotal, isLast, isFirst) {
        var postData = {
            page: pageData.page,
            pagesize: pageData.pagesize,
            getTotal: getTotal
        }

        if (isLast && isLast == "yes") {
            postData.page = pageData.lastPage;
        } else if (isFirst && isFirst == "yes") {
            postData.page = 1;
        }

        server_api.getExtendserverList(JSON.stringify(postData), function (resp) {
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
        var colsList = [
            {checkbox: true},
            {field: 'id', align: 'left', title: '序号', width: 80},
            {field: 'server_ip', align: 'left', title: '服务器IP', width: 160,edit:"text"},
            {field: 'server_type', align: 'left', title: '服务器类型', width: 180},
            {field: 'server_name', align: 'left', title: '服务器名字', cellMinWidth: 180,edit:"text"},
            {field: 'login_name', align: 'left', title: '登录用户名', width: 200,edit:"text"},
            {field: 'login_pwd', align: 'left', title: '登录密码', width: 200,edit:"text"},
            // {field: 'create_time', align: 'left', title: '操作时间', cellMinWidth: 140, sort: true, event: "create_time"},
        ]

        var renderObj ={
            id: "serverList",//
            elem: '#serverList',//指定表格元素
            data: dataList,  //表格当前页面数据
            // height: 'full-20',
            limit: pageData.pagesize,
            toolbar: '#toolbarAction',
            defaultToolbar: ['filter'],
            cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
            skin: 'line ', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
            done: function (res, curr, count) {
            },
            cols: [colsList]
        }

        table.render(renderObj)
    }


    //监听单元格编辑
    table.on('edit(serverList)', function (obj) {
        var value = obj.value //得到修改后的值
            , data = obj.data //得到所在行所有键值
            , field = obj.field; //得到字段

        if (!value || value.length == 0) {
            layer.msg("不能修改为空!", {icon: 2})
            setCurrentData("yes")
            return
        } else {
            var postData = data
            postData[field] = value
            server_api.addOrUpdateServerInfo(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    layer.msg("修改成功!", {icon: 1})
                } else {
                    layer.msg("修改失败!", {icon: 2})
                }
            })
        }
    })

    table.on('toolbar(serverList)', function (obj) {
        if(obj.event == "addServer"){
            server_api.addOrUpdateServerInfo(JSON.stringify(defaultServer), function (resp) {
                if (resp.status == 0) {
                    layer.msg("添加成功!", {icon: 1})
                    setCurrentData("yes")
                } else {
                    layer.msg("添加失败!", {icon: 2})
                }
            })
        }else if(obj.event == "batchDelete"){
            var data = table.checkStatus('serverList').data;
            if (data && data.length > 0) {
                layer.confirm('是否真的删除勾选的任务?', {icon: 3, title: '提示'}, function (index) {
                    var server_ips = []
                    for (var i = 0; i < data.length; i++) {
                        server_ips.push(data[i].server_ip)
                    }
                    var postData = {
                        server_ips: server_ips
                    }
                    server_api.deleteServerInfo(JSON.stringify(postData),function (resp) {
                        if(resp.status==0){
                            layer.msg("删除成功!",{icon:1})
                            setCurrentData("yes")
                        }else{
                            layer.msg("删除失败!",{icon:2})
                        }
                    })
                })
            }


        }


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
        setCurrentData("yes")
    })


})