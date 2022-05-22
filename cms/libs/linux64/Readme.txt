【说明】
	Demo以Qt开发，在CentOS上进行编译，以下以CentOS系统为例，说明的Demo使用方式，其他Linux版本参考CentOS系统。

如果SDK各动态库文件和可执行文件在同一级目录下，则使用同级目录下的库文件;此时也可能报找不到库文件的错误，需要显示设置一下环境变量：export  LD_LIBRARY_PATH=$LD_LIBRARY_PATH:./

如果不在同一级目录下，则需要将以上文件的目录加载到动态库搜索路径中，设置的方式有以下几种:
一.	将网络SDK各动态库路径加入到LD_LIBRARY_PATH环境变量
	1.在终端输入：export  LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/XXX      只在当前终端起作用
	2. 修改~/.bashrc或~/.bash_profile，最后一行添加 export  LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/XXX，保存之后，使用source  .bashrc执行该文件 ，当前用户生效
	3. 修改/etc/profile，添加内容如第2条，同样保存之后使用source执行该文件  所有用户生效

二．在/etc/ld.so.conf文件结尾添加网络sdk库的路径，如/XXX，保存之后，然后执行ldconfig 

三．可以将网络sdk各依赖库放入到/lib64、/lib或usr/lib64、usr/lib下

四．可以在Makefile中使用-Wl,-rpath来指定动态路径，直接将dhnetsdk库以–l方式显示加载进来
	比如：-Wl,-rpath=/XXX -lhdhnetsdk 


推荐使用一或二的方式，但要注意优先使用的是同级目录下的库文件。
