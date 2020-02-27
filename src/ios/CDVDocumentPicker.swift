import UIKit
import Photos

@objc(CDVDocumentPicker)
class CDVDocumentPicker : CDVPlugin {
    var commandCallback: String?
    var isMultiple: Bool = false
    var isCompress: Bool = true
    @objc(getFile:)
    func getFile(command: CDVInvokedUrlCommand) {
        DispatchQueue.global(qos: .background).async {
			//srcType:PHOTOLIBRARY,  SAVEDPHOTOALBUM, DOCUMENT
			//fileTypes: 可是以单个字符串或数组
			//title:   弹出框的Title,IOS不需要
            var srcType: String  = ""
            var fileTypes: [String] = []
            self.isCompress = true //true 要压缩
			self.commandCallback = command.callbackId
            
			if command.arguments.isEmpty || command.arguments.count < 2{
				self.sendError("Didn't receive all arguments.")
			} else {
                if( command.arguments[0] as? String != nil){
                    srcType = command.arguments[0] as! String
                     if  srcType != "PHOTOLIBRARY" && srcType  != "SAVEDPHOTOALBUM" && srcType  != "DOCUMENT" {
                        srcType = "DOCUMENT"
                    }
                }
                
                if let key = command.arguments[1] as? String {
                        let type  = self.formatDocType(fileType: key)
                        fileTypes.append(type)
                } else if let array = command.arguments[1] as? [String] {
                    fileTypes = array.compactMap { self.formatDocType(fileType: $0) }
                }

				if fileTypes.isEmpty {
                    fileTypes.append("*/*")
					//self.sendError("Didn't receive any filetypes argument.")
				}
                
                self.isMultiple =  command.arguments[3] as! Bool
                if command.arguments.count>4{
                    self.isCompress = command.arguments[4] as! Bool
                }
					
                if srcType == "DOCUMENT" {
                    self.callPicker(withTypes: fileTypes, multiple: self.isMultiple)
                } else {
                    self.callImagePicker(srcType:srcType, withTypes: fileTypes, multiple: self.isMultiple)
                }
				
			}
        }
    }
    
	func formatDocType(fileType: String) -> String {
        switch fileType {
			case "*/*":
				return "public.data"
			case "video/*":
				return "public.movie"
			case "video/mp4":
				return "public.mpeg-4"
			case "video/avi":
				return "public.avi"				
			case "image/*":
				return "public.image"
			case "image/gif":
				return "com.compuserve.gif"
			case "image/jpeg":
				return "public.jpeg"
			case "image/png":
				return "public.png"
			case "audio/*":
				return "public.audio"
			case "audio/mp3":
				return "public.mp3"
			case "application/pdf", "pdf":
				return "com.adobe.pdf"
			case "application/msword", "doc":
				return "com.microsoft.word.doc"
			case "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx":
				return "org.openxmlformats.wordprocessingml.document"
			case "application/vnd.ms-excel", "xls":
				return "com.microsoft.excel.xls"
			case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx":
				return "org.openxmlformats.spreadsheetml.sheet"
			case "application/mspowerpoint", "ppt","application/vnd.ms-powerpoint":
                  return "com.microsoft.powerpoint.ppt"
			case "application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx":
				return "org.openxmlformats.presentationml.presentation"
			default:
				return fileType
		}	
	}
	
    func callPicker(withTypes documentTypes: [String], multiple isMultiple:Bool) {
	
        DispatchQueue.main.async {

            let picker = UIDocumentPickerViewController(documentTypes: documentTypes, in: .import)  //.import //open
            picker.delegate = self
            if #available(iOS 11.0, *) {
                picker.allowsMultipleSelection = isMultiple
            } else {
                // Fallback on earlier versions
            };

            self.viewController.present(picker, animated: true, completion: nil)
        }
    }
	
    func callImagePicker(srcType: String, withTypes documentTypes: [String], multiple isMultiple:Bool) {

        DispatchQueue.main.async {
			let imagePickerController = UIImagePickerController()
			//设置代理
			imagePickerController.delegate = self
			//允许用户对选择的图片或影片进行编辑
			imagePickerController.allowsEditing = false
			//设置image picker的用户界面
			imagePickerController.sourceType = srcType == "PHOTOLIBRARY" ? .photoLibrary : .savedPhotosAlbum //或者.savedPhotosAlbum
			imagePickerController.mediaTypes =  documentTypes   //[kUTTypeMovie as String]
            imagePickerController.videoQuality = UIImagePickerControllerQualityType.typeHigh
            //imagePickerController.for
           // imagePickerController.mul = isMultiple;
			//设置图片选择控制器导航栏的背景颜色
		  //  imagePickerController.navigationBar.barTintColor = UIColor.orange
			//设置图片选择控制器导航栏的标题颜色
		 //   imagePickerController.navigationBar.titleTextAttributes = [NSAttributedStringKey.foregroundColor: UIColor.white]
			//设置图片选择控制器导航栏中按钮的文字颜色
		//    imagePickerController.navigationBar.tintColor = UIColor.white
			//显示图片选择控制器
            if #available(iOS 11.0, *) , self.isCompress==false {
                imagePickerController.videoExportPreset = AVAssetExportPresetPassthrough
            }
            
            self.viewController.present(imagePickerController, animated: true, completion: nil)

        }
    }
	
    func documentWasSelected(document: URL) {
        self.sendResult(.init(status: CDVCommandStatus_OK, messageAs: document.absoluteString))
        self.commandCallback = nil
    }
    
    func documentsWasSelected(documents: [URL]) {
        var str_urls: String = "["
        var i: Int = 0
        for u in documents {
            str_urls.append("'" + u.absoluteString.replacingOccurrences(of: "'", with: "\\'", options: String.CompareOptions.caseInsensitive) + "'")
            if(i<documents.count-1){
                str_urls.append(",")
            }
            i+=1
        }
        str_urls.append("]")
        self.sendResult(.init(status: CDVCommandStatus_OK, messageAs: str_urls))
        self.commandCallback = nil
       }

    func sendError(_ message: String) {
        sendResult(.init(status: CDVCommandStatus_ERROR, messageAs: message))
    }

}

private extension CDVDocumentPicker {
    func sendResult(_ result: CDVPluginResult) {

        self.commandDelegate.send(
            result,
            callbackId: commandCallback
        )
    }
}

extension CDVDocumentPicker: UIDocumentPickerDelegate {

    @available(iOS 11.0, *)
    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        if(urls.count > 1 && self.isMultiple){
            documentsWasSelected(documents: urls)
        }else {
            if let url = urls.first {
                documentWasSelected(document: url)
            }
        }
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentAt url: URL){
        documentWasSelected(document: url)
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        sendError("User canceled.")
    }
}

extension CDVDocumentPicker: UIImagePickerControllerDelegate, UINavigationControllerDelegate{
  //选择图片成功后代理
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [String : Any]) {
 
//       let group = DispatchGroup();
//        let  queueRequest = DispatchQueue.global();
//
//       queueRequest.async(group:group){
            
        //选择图片的引用路径
            var pickedURL = info["UIImagePickerControllerImageURL"] as? URL
    //        if  pickedURL == nil {
    //            pickedURL =  info["UIImagePickerControllerReferenceURL"] as? URL
    //        }
    //
            if pickedURL == nil  {
                pickedURL =  info["UIImagePickerControllerMediaURL"] as? URL
            }
        if #available(iOS 13.0, *), self.isCompress {
                pickedURL = self.createTemporaryURLforVideoFile(url: pickedURL as! URL)
        }
        
        if #available(iOS 13.0, *), self.isCompress == false {
                if let phasset = info["UIImagePickerControllerPHAsset"] as? PHAsset {
                   PHCachingImageManager().requestAVAsset(forVideo: phasset as PHAsset, options:nil, resultHandler: { (asset, audioMix, info) in

                        let avurl = asset as! AVURLAsset;
                        self.documentWasSelected(document: avurl.url )
                   })
                }
        }
        else{
            if pickedURL == nil {
                self.sendError("No File selected.")
            }

            self.documentWasSelected(document: pickedURL ?? info[UIImagePickerControllerReferenceURL] as! URL)
        }


        //图片控制器退出
        picker.dismiss(animated: true, completion:{})
    }
     
	func imagePickerControllerDidCancel(_ picker: UIImagePickerController){
	 //图片控制器退出
        picker.dismiss(animated: true, completion:nil)
		sendError("User canceled.")
	}
    
     func createTemporaryURLforVideoFile(url: URL) -> URL {
        /// Create the temporary directory.
        let temporaryDirectoryURL = URL(fileURLWithPath: NSTemporaryDirectory(), isDirectory: true)
        /// create a temporary file for us to copy the video to.
        let temporaryFileURL = temporaryDirectoryURL.appendingPathComponent(url.lastPathComponent)
        /// Attempt the copy.
        do {
            try FileManager().copyItem(at: url.absoluteURL, to: temporaryFileURL)
        } catch {
            print("There was an error copying the video file to the temporary location.")
        }

        return temporaryFileURL as URL
    }
	
//    override func didReceiveMemoryWarning() {
//        super.didReceiveMemoryWarning()
//    }

}
