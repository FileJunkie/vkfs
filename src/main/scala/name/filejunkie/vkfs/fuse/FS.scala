package name.filejunkie.vkfs.fuse

import jnr.ffi.Pointer
import jnr.ffi.types.off_t
import name.filejunkie.vkfs.vk.VkApi
import ru.serce.jnrfuse.FuseFillDir
import ru.serce.jnrfuse.FuseStubFS
import ru.serce.jnrfuse.struct.FileStat
import ru.serce.jnrfuse.struct.FuseFileInfo
import ru.serce.jnrfuse.ErrorCodes

class FS(userId: String) extends FuseStubFS {
  val vkApi = new VkApi(userId)
  
  override def getattr(path: String, stat: FileStat) : Int = {
    stat.st_mode.set(FileStat.S_IFDIR | 493) // 0775 oct
    stat.st_nlink.set(2)
    0
  }
  
  override def readdir(path: String, buf: Pointer, filter: FuseFillDir, @off_t offset: Long, fi: FuseFileInfo) : Int = {
    if(path != "/") -ErrorCodes.ENOENT()
    
    filter.apply(buf, ".", null, 0)
    filter.apply(buf, "..", null, 0)
    
    vkApi.getAlbums.foreach { album => filter.apply(buf, album.title, null, 0) }        
    
    0
  }
}