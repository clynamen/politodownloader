package io.clynamen.github.PolitoDownloader

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

  def courses(year : Integer) = String.format(CoursesListUrlFormat, year)

}
