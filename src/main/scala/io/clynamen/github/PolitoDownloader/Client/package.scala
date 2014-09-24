package io.clynamen.github.PolitoDownloader

import com.github.pathikrit.dijon.SomeJson

package object ClientUri {
  val PortalHost = "https://didattica.polito.it"
  val PortalLoginHost = "https://login.didattica.polito.it"
  val IdpHost = "https://idp.studenti.polito.it"

  // just use some page that returns 200 if logged in
  val ProfilePhotoUri = PortalHost + "/portal/pls/portal/sviluppo.foto_studente?p_matricola="
  val CourseVideoIndex = PortalHost + "/pls/portal30/sviluppo.videolezioni.vis"
  val ShibLogin = PortalLoginHost + "/secure-studenti/ShibLogin.php"
  val UserPasswordLogin = IdpHost + "/idp/Authn/X509Mixed/UserPasswordLogin"
  private val CoursesListUrlFormat = PortalHost + "/pls/portal30/sviluppo.materiale.elenco?a=%d&t=M"
  private val VideoCoursesListUrlFormat = PortalHost + "/portal/pls/portal/sviluppo.materiale.elenco?a=%d&t=E"
  // https://didattica.polito.it/pls/portal30/sviluppo.videolezioni.vis?cor=68
  private val typeAVideoListFormat = PortalHost + "/pls/portal30/sviluppo.videolezioni.vis?cor=%d"
  private val typeBVideoListFormat = PortalHost + "/pls/portal30/sviluppo.materiale.elarning?inc=%d&n=1"
  private val typeCVideoListFormat =
 "http://elearning.polito.it/gadgets/video/template_video.php?utente=%s&inc=%s&data=%s&token=%s"
  private val typeCVideoListAccessFormat = PortalHost + "/pls/portal30/sviluppo.materiale.json_dokeos_par?inc=%d"

  def courses(year : Integer) = String.format(CoursesListUrlFormat, year)
  def videoCourses(year : Integer) = String.format(VideoCoursesListUrlFormat, year)

  def typeAVideoList(id: Integer) = String.format(typeAVideoListFormat, id)
  def typeBVideoList(id: Integer) = String.format(typeBVideoListFormat, id)
  def typeCVideoListAccess(id: Integer) = String.format(typeCVideoListAccessFormat, id)
  // TODO: use another json library (and pass plain dict here)
  def typeCVideoList(formData: SomeJson) = String.format(typeCVideoListFormat,
              removeQuotes(formData.utente.toString),
              removeQuotes(formData.inc.toString),
              removeQuotes(formData.data.toString),
              removeQuotes(formData.token.toString))

  private def  removeQuotes(s: String) = s.replaceAll("\"", "")

}
