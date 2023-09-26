import zio.test.*
import zio.*
import fi.kimmoeklund.service.RoleRepository
import fi.kimmoeklund.domain.*
import fi.kimmoeklund.service.PermissionRepository
import fi.kimmoeklund.html.pages.RolesPage
import java.util.UUID

val roles = Seq(Role(RoleId(UUID.randomUUID()), "unittest", List.empty), Role(RoleId(UUID.randomUUID()), "unittest2", List.empty))

object RoleRepositoryMock extends RoleRepository:
  override def getRoles: IO[GetDataError, Seq[Role]] = ZIO.succeed(roles)
  override def addRole(role: Role): IO[ErrorCode, Role] = ???
  override def getRolesByIds(ids: Seq[RoleId]): IO[GetDataError, List[Role]] = ???
  override def deleteRole(id: UUID): IO[ErrorCode, Unit] = ???
  override def updateUserRoles: IO[ErrorCode, Option[User]] = ???
  def layer = ZLayer.succeedEnvironment(ZEnvironment(Map(("unittest", this.asInstanceOf[RoleRepository]))))

object PermissionRepositoryMock extends PermissionRepository {
  override def addPermission(permission: Permission): IO[ErrorCode, Permission] = ???
  override def getPermissions: IO[GetDataError, List[Permission]] = ???
  override def deletePermission(id: UUID): IO[ErrorCode, Unit] = ???
  override def getPermissionsByIds(ids: Seq[UUID]): IO[GetDataError, List[Permission]] = ???
  def layer = ZLayer.succeedEnvironment(ZEnvironment(Map(("unittest", this.asInstanceOf[PermissionRepository]))))
}

object RolePageSpec extends ZIOSpecDefault:
  val rolePage = RolesPage("/roles", "unittest")

//
// -- setup
//

  override def spec = suite("RolePage")(
//    test("it should list options") {
//      for {
//        options <- rolePage.optionsList()
//      } yield assertTrue(options.encode == s"""<option value="${roles(0).id}">unittest</option><option value="${roles(1).id}">unittest2</option>""")
//    },
//    test("it should list option with selected attribute") {
//      for {
//        options <- rolePage.optionsList(Some(Seq(roles(0).id.toString)))
//      } yield assertTrue(options.encode == s"""<option value="${roles(0).id}" selected="true">unittest</option><option value="${roles(1).id}" selected="true">unittest2</option>""")
//
//    },
    test("it should list two options with selected attribute") {
      for {
        options <- rolePage.optionsList(Some(Seq(roles(0).id.toString, roles(1).id.toString)))
      } yield assertTrue(options.encode == s"""<option value="${roles(0).id}" selected="true">unittest</option><option value="${roles(1).id}" selected="true">unittest2</option>""")

    }
  ).provide(RoleRepositoryMock.layer ++ PermissionRepositoryMock.layer)

  //.provider(provideCustomLayerShared(RoleRepositoryMock ++ PermissionRepositoryMock))

end RolePageSpec
