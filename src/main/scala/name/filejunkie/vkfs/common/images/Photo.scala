package name.filejunkie.vkfs.common.images

case class Photo (
  id: Long,
  album_id: Long,
  owner_id: Long,
  photo_75: Option[String],
  photo_130: Option[String],
  photo_604: Option[String],
  photo_807: Option[String],
  photo_1280: Option[String],
  photo_2560: Option[String],
  width: Option[Long],
  height: Option[Long],
  text: String,
  date: Long,
  likes: Option[Likes],
  comments: Option[Comments],
  can_comment: Option[Short],
  tags: Option[Tags])

case class Likes(user_likes: Integer, count: Integer);
case class Comments(count: Integer);
case class Tags(count: Integer);