package name.filejunkie.vkfs.common.images

case class Album(id: Long,
  thumb_id: Long,
  owner_id: Long,
  title: String,
  description: String,
  created: Long,
  updated: Long,
  size: Long,
  thumb_is_last: Short,
  privacy_view: List[String],
  privacy_comment: List[String])