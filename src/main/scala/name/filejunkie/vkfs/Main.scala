package name.filejunkie.vkfs

import name.filejunkie.vkfs.vk.VkApi
import name.filejunkie.vkfs.fuse.FS
import java.nio.file.Paths

object Main {
  def main(args: Array[String]) = {
    val mountDir = args(0)
    val userId = args(1)

    val fs = new FS(userId)
    try{
      fs.mount(Paths.get(mountDir), true)
    }
    finally{
      fs.umount()
    }
  }  
}