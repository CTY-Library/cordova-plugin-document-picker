### 文件选择插件

## 参考
### https://github.com/iampossible/Cordova-DocPicker.git
### cordova-plugin-camera

使用

`uploadVideo(){`
    this.mediaType = this.camera.MediaType.VIDEO;
    this.currentType = 'VIDEO';
    CDVDocumentPicker.getFile( (url) => { alert(url);
      if(this.Util.isIOS){
        if(url.indexOf("file:///")>= 0){
           url = url.substr(8, url.length - 8);//去掉 file:///
        }
        let fileName = this.generateFileName('.mp4');
      AppAliYunOSS.uploadFile(this.memberApi, 'teamvideo', fileName, url, (objectName) => {
        this.onSuccess(objectName , fileName); //objectName为阿里上的地址
         
      }, (err) => {
        alert('错误1:'+JSON.stringify(err));        
      });
      }   
    }, (error) => { alert(error) },"SAVEDPHOTOALBUM",['video/*'],"");
  }
  uploadDocument(){
    this.mediaType = this.camera.MediaType.VIDEO;
    this.currentType = 'VIDEO';
    CDVDocumentPicker.getFile( (url) => { 
      url = decodeURIComponent(url); //转码
      alert(url);
      if(this.Util.isIOS){
        if(url.indexOf("file:///")>= 0){
           url = url.substr(8, url.length - 8);//去掉 file:///
        }
        let fileName = this.generateFileName('.mp4');
      AppAliYunOSS.uploadFile(this.memberApi, 'teamvideo', fileName, url, (objectName) => {
        this.onSuccess(objectName , fileName); //objectName为阿里上的地址
         
      }, (err) => {
        alert('错误1:'+JSON.stringify(err));     
      });
      }
    }, (error) => { alert(error) },"DOCUMENT",['*/*'],"");
  }`
