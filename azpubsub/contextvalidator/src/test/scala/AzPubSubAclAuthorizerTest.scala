import java.util.Date

import kafka.metrics.KafkaMetricsGroup
import kafka.network.RequestChannel.Session
import kafka.security.auth.{Acl, All, Allow, AzPubSubAclAuthorizer, Resource, Topic}

import scala.collection.mutable
import org.apache.kafka.common.security.auth.KafkaPrincipal
import org.easymock.EasyMock
import org.easymock.EasyMock._
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.support.membermodification.MemberMatcher
import org.powermock.core.classloader.annotations.{PowerMockIgnore, PrepareForTest}
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.Whitebox
import org.powermock.api.support.membermodification.MemberModifier.suppress


@RunWith(classOf[PowerMockRunner])
@PrepareForTest(Array(classOf[org.slf4j.LoggerFactory], classOf[AzPubSubAclAuthorizer], classOf[KafkaMetricsGroup]))
@PowerMockIgnore(Array("javax.management.*"))
class AzPubSubAclAuthorizerTest {

  @Test
  def testAzPubSubAclAuthorizerAuthorizeTokenPositive(): Unit = {
        val tokenJsonString = "{" +
          "\"Roles\":" +
          "[" +
          "\"ToAuthenticateAzPubSub\"" +
          "]," +
          "\"ValidFrom\":\"6/18/2019 7:08:11 AM\"," +
          "\"ValidTo\":\"6/19/2079 7:08:11 AM\"," +
          "\"UniqueId\":\"3efcea45-7aae-4fb9-af22-31d6cf9f0b20\"," +
          "\"Base64Token\":" +
            "\"PEFzc2VydGlvbiBJRD0iXzdkYzNmOWU2LWJhOGUtNDQzNS04ZGM2LTNhMTk0YTRiMzFjOSIgSXNzdWVJbnN0YW50PSIyMDE5LTA2LTE4VDA3OjA4OjExLjUzNFoiIFZlcnNpb249IjIuMCIgeG1sbnM9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iPgogIDxJc3N1ZXI+cmVhbG06Ly9zYW1wbGVyZWFsbS5uZXQvPC9Jc3N1ZXI+CiAgPGRzOlNpZ25hdHVyZSB4bWxuczpkcz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI+CiAgICA8ZHM6U2lnbmVkSW5mbz4KICAgICAgPGRzOkNhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiIC8+CiAgICAgIDxkczpTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzA0L3htbGRzaWctbW9yZSNyc2Etc2hhMjU2IiAvPgogICAgICA8ZHM6UmVmZXJlbmNlIFVSST0iI183ZGMzZjllNi1iYThlLTQ0MzUtOGRjNi0zYTE5NGE0YjMxYzkiPgogICAgICAgIDxkczpUcmFuc2Zvcm1zPgogICAgICAgICAgPGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNlbnZlbG9wZWQtc2lnbmF0dXJlIiAvPgogICAgICAgICAgPGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIgLz4KICAgICAgICA8L2RzOlRyYW5zZm9ybXM+CiAgICAgICAgPGRzOkRpZ2VzdE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZW5jI3NoYTI1NiIgLz4KICAgICAgICA8ZHM6RGlnZXN0VmFsdWU+UHV5Z0pWSTBobEI4TDE2YmFsb09MOHJEdHgzQ2RnOS94VkRzYUx2aXo5VT08L2RzOkRpZ2VzdFZhbHVlPgogICAgICA8L2RzOlJlZmVyZW5jZT4KICAgIDwvZHM6U2lnbmVkSW5mbz4KICAgIDxkczpTaWduYXR1cmVWYWx1ZT5IcnFzNm52dkpFMElsNEZvUzBmYUkyc1JFQ3hOdFFLZDRKZ0tVcTIwd21wODJCajU1bEZhY1h3ZndoWUlSTUV2eUNVK2pZVHZZRXNqVVpZUEdCR3JlOENPWkt3Z2FYTDM5R0g1RG53WEpHSURRc3RocCtvM05CeFpFQ09sODVEVWhaZ2k0akhPR1pjRFpFalVXNXM4Z1d4a2FManBlakc2Y3lRUytiaHRBTVBxK1ByS3JPVG1jaEkrTStFM0g5eUpnYys2RlJQdjVldUc4UDZzd2lOb2QzVmFvY2tsdHE2S1VQdXNhbFFtTTllaklxdDVBdmlQb3VwWGx4WHI1RTZ5VENnMGxhR1B5WFhGbEljUWJyYWNQcDBuekl0MkR1RU1TS2wvbnlMS2JISkRKdDRGRGtLYktyb243aityblNqdnFXRmd0cElxT09ZWHF1ZWIxbTNZTUE9PTwvZHM6U2lnbmF0dXJlVmFsdWU+CiAgICA8S2V5SW5mbyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI+CiAgICAgIDxYNTA5RGF0YT4KICAgICAgICA8WDUwOUNlcnRpZmljYXRlPk1JSUpOVENDQngyZ0F3SUJBZ0lUSUFBRVcybUJEdGxDelg0T2t3QUFBQVJiYVRBTkJna3Foa2lHOXcwQkFRc0ZBRENCaXpFTE1Ba0dBMVVFQmhNQ1ZWTXhFekFSQmdOVkJBZ1RDbGRoYzJocGJtZDBiMjR4RURBT0JnTlZCQWNUQjFKbFpHMXZibVF4SGpBY0JnTlZCQW9URlUxcFkzSnZjMjltZENCRGIzSndiM0poZEdsdmJqRVZNQk1HQTFVRUN4TU1UV2xqY205emIyWjBJRWxVTVI0d0hBWURWUVFERXhWTmFXTnliM052Wm5RZ1NWUWdWRXhUSUVOQklESXdIaGNOTVRneE1ERXhNakl6TkRVd1doY05NakF4TURFeE1qSXpORFV3V2pBbE1TTXdJUVlEVlFRRERCb3FMbVJ6ZEhNdVkyOXlaUzVoZW5WeVpTMTBaWE4wTG01bGREQ0NBU0l3RFFZSktvWklodmNOQVFFQkJRQURnZ0VQQURDQ0FRb0NnZ0VCQUozRnU1UDRBQnRxTk53S0YxS0VZSkxGc1hMMWU2cnhxN3ZjM3dEd0pjWHBWUWpRM0IwVHpVd0Zqc0J4S2VOamhwUzlrcFk2Y1pqNVEyWkgvREdzUUZVNEZqK2xtYlJuQXRQWTNXT3NwM3lyWEJUTWFEQkFySkNxYXdCMFBHT0ZPWStwZWdkZnhPR0JTSVp1Q29vWkpOOVB0T3h3OVk4RUgzOC90NnczU0prNzBOb1N2cFhhdU5Gc2lyT3FjNkpTRktJSEN0STdJdDZSTEtnOTM1b3YvY2xrd3NPNFMxTDcvZDIvWnowenBGNWVBNU1xWUU5Wlg3TUU4akZMNU5oWUpjYU1jWXFSZUpTVlc2N2Q2eGt4T1huaCtVL0JBTDE1ZnlYYTJOU0x1S0dnd3JneitYVHJkUi9YUytwQlpoSEplSmYyOWw1ekJvUlNzd2NzenhQY0FMOENBd0VBQWFPQ0JQVXdnZ1R4TUlJQjlRWUtLd1lCQkFIV2VRSUVBZ1NDQWVVRWdnSGhBZDhBZGdDa3VRbVF0QmhZRkllN0U2TE1aM0FLUERXWUJQa2IzN2pqZDgwT3lBM2NFQUFBQVdabFRkazRBQUFFQXdCSE1FVUNJUURIZUo3OTFCYTdKS0dXaWYwa1E1ajNLWFhOU2dhYW5tMXdBWk9BZm84WTJ3SWdGQ245Q0l0bUM0aDY3RmczcTZmaG92U2tSeUpLTmdHSTlVTS9JRXNLNXlrQWRRRHdsYVJaOGdEUmdrQVFMUytUaUk2dFMvNGRSK09aNGRBMHByQ29xbzZ5Y3dBQUFXWmxUZGxLQUFBRUF3QkdNRVFDSUZxQnNUSkMreHZ0Q01oaVRSbkUzRm8vQldtcDZYNDV1TnRNWUsrRmpPTHBBaUFlek1oZVdjaitHem5sd0M4TTcrU2RVUFFTaWJ0aitqbmlpcHFUVGJ0MjlRQjNBRjZuYy9uZlZzRG50VFpJZmRCSjRESjZrWm9NaEtFU0VvUVlkWmFCY1VWWUFBQUJabVZOMlZRQUFBUURBRWd3UmdJaEFMWWpEZ3o3djRSUTFtS2d5SCtuaUpzS05mL2RlMFRqOVFiZ2N5RkxUZEtLQWlFQS8rNWN1bjRyUkxTSXV3ZHR5enowNlJ5M1BEcXhDMDJucHdFUks3VitMb1VBZFFCVmdkVENGcEEyQVVycUM1dFhQRlB3d09RNGVIQWxDQmN2bzZvZEJ4UFREQUFBQVdabFRkcy9BQUFFQXdCR01FUUNJQ1BXUWd2UE9HREpadlRrV2daNmljZjBMdElwN3o4OG9FVmI1MDFOMXdzeUFpQnNGalBTUTAwS0JURjE0aVh1RmNGWW9ObWU1bmFCRGxNRVRyaGhrQ2Zzb3pBbkJna3JCZ0VFQVlJM0ZRb0VHakFZTUFvR0NDc0dBUVVGQndNQ01Bb0dDQ3NHQVFVRkJ3TUJNRDRHQ1NzR0FRUUJnamNWQndReE1DOEdKeXNHQVFRQmdqY1ZDSWZhaG5XRDd0a0Jnc21GRzRHMW5tR0Y5T3RnZ1YyRTB0OUNndWVUZWdJQlpBSUJIVENCaFFZSUt3WUJCUVVIQVFFRWVUQjNNRkVHQ0NzR0FRVUZCekFDaGtWb2RIUndPaTh2ZDNkM0xtMXBZM0p2YzI5bWRDNWpiMjB2Y0d0cEwyMXpZMjl5Y0M5TmFXTnliM052Wm5RbE1qQkpWQ1V5TUZSTVV5VXlNRU5CSlRJd01pNWpjblF3SWdZSUt3WUJCUVVITUFHR0ZtaDBkSEE2THk5dlkzTndMbTF6YjJOemNDNWpiMjB3SFFZRFZSME9CQllFRkVVbnREWTgyQTNTUTc0SkJBNlRua2tLSS9DZE1Bc0dBMVVkRHdRRUF3SUVzRENCbWdZRFZSMFJCSUdTTUlHUGdob3FMbVJ6ZEhNdVkyOXlaUzVoZW5WeVpTMTBaWE4wTG01bGRJSWJLaTVrYzNSekxtTnZjbVV1ZDJsdVpHOTNjeTFwYm5RdWJtVjBnaHNxTG1SemRITXVZMjl5WlM1M2FXNWtiM2R6TFhSemRDNXVaWFNDSFNvdVpITjBjeTVsTW1WMFpYTjBNaTVoZW5WeVpTMXBiblF1Ym1WMGdoZ3FMbVJ6ZEhNdWFXNTBMbUY2ZFhKbExXbHVkQzV1WlhRd2dhd0dBMVVkSHdTQnBEQ0JvVENCbnFDQm02Q0JtSVpMYUhSMGNEb3ZMMjF6WTNKc0xtMXBZM0p2YzI5bWRDNWpiMjB2Y0d0cEwyMXpZMjl5Y0M5amNtd3ZUV2xqY205emIyWjBKVEl3U1ZRbE1qQlVURk1sTWpCRFFTVXlNREl1WTNKc2hrbG9kSFJ3T2k4dlkzSnNMbTFwWTNKdmMyOW1kQzVqYjIwdmNHdHBMMjF6WTI5eWNDOWpjbXd2VFdsamNtOXpiMlowSlRJd1NWUWxNakJVVEZNbE1qQkRRU1V5TURJdVkzSnNNRTBHQTFVZElBUkdNRVF3UWdZSkt3WUJCQUdDTnlvQk1EVXdNd1lJS3dZQkJRVUhBZ0VXSjJoMGRIQTZMeTkzZDNjdWJXbGpjbTl6YjJaMExtTnZiUzl3YTJrdmJYTmpiM0p3TDJOd2N6QWZCZ05WSFNNRUdEQVdnQlNSbmp0RWJEMVhuRUozS2pUWFQ5SE1TcGNzMmpBZEJnTlZIU1VFRmpBVUJnZ3JCZ0VGQlFjREFnWUlLd1lCQlFVSEF3RXdEUVlKS29aSWh2Y05BUUVMQlFBRGdnSUJBRTAvMGU0bmVxOHc0dE5WR3pzam5LUW1tVm5FMGlnUmtxMVNTbkk0TDJXenBKN1V0a2J1RWZmZ1BKRVpHZkt5aGRzVVVFR1VlZ0NMWkVmbWk0d2ZoV21tdWcxZ2dVOFdLbVZ5NTEzejUyY1h6YkpQM3NNYXNaZno0UkJ1YlZKdWlMUzJ1VG5EMkpObGV2cEwzdkJHR1Z2T2lCb1gyakhDbDJGNUdNV0wvZTdKaUtKOWtNenNENWpidktBNFJDQ0h6NVFmL0tnU0Z6ZU1qSHFQSnFCQmV4bk0wQ3dJWlBhaCtMYWcyb1RLZGttZlFpZnRzSjZwNnE3ZVI5ejRRMkl4U1JQbGhmRThLWkMxZExCd1NOMUgySFhkWDZ0SW1QSVBtNnd3eGd1eVJab0JzSC8xRTY3N0h1VWFKUEE0eFdOTkcwaXdOR044TnNLek5OMEdkMHN4OW56YkhsYlFEZEJLVFlIMUNDRk16TmNTdUJqaU1hOFQ3c3pTVStoeDFwRmplYWlYTFJ6d1pqdUlkeDQ1MnQ4b0RKY0VZNEI3KzZEQm9uTGs4TVlTNGZsUm5wcGZabnB3TERNcVVVZFRNNkJjRExGOXNhdlV1dzhaOG9OUm1jQjEwTlB3ZEplYzBzRWRoSDNNRThvVkFoWjZ2dHRFa2lLQmhPZXZjY2w4Umh1ekE1YnVjZFc2M0RkQVFPWDJGUzFoNG0vRGxIeU9PMFA3NG5WdTkzQUlnc0hPVGsvbFlxWnZXUVh5dmg4NHk4U1FQRm1wejBXOHduT1pIMUdXTXRXMkt2em8vU2phb2ZoU0NYa0FWa3hBN1dGU2llL2pCUlRxVzc0WWsxQTRIRHpCaml6UlZJM0FKRCtiWldjMGNFSlV0NHBOQ3pVQmY3M2crU0pKOWErUDVvbkI8L1g1MDlDZXJ0aWZpY2F0ZT4KICAgICAgPC9YNTA5RGF0YT4KICAgIDwvS2V5SW5mbz4KICA8L2RzOlNpZ25hdHVyZT4KICA8U3ViamVjdD4KICAgIDxOYW1lSUQ+c2FtcGxlLnRlc3QuYXp1cmUubmV0PC9OYW1lSUQ+CiAgICA8U3ViamVjdENvbmZpcm1hdGlvbiBNZXRob2Q9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpjbTpiZWFyZXIiIC8+CiAgPC9TdWJqZWN0PgogIDxDb25kaXRpb25zIE5vdEJlZm9yZT0iMjAxOS0wNi0xOFQwNzowODoxMS41MThaIiBOb3RPbk9yQWZ0ZXI9IjIwNzktMDYtMTlUMDc6MDg6MTEuNTE4WiI+CiAgICA8QXVkaWVuY2VSZXN0cmljdGlvbj4KICAgICAgPEF1ZGllbmNlPnN2YzovL0F6UHViU3ViU2VydmljZVRlc3RAc2FtcGxlLWRzdHMtYXdhcmUtc2VydmljZS5jb3JlLndpbmRvd3MtdHN0Lm5ldC88L0F1ZGllbmNlPgogICAgPC9BdWRpZW5jZVJlc3RyaWN0aW9uPgogIDwvQ29uZGl0aW9ucz4KICA8QXR0cmlidXRlU3RhdGVtZW50PgogICAgPEF0dHJpYnV0ZSBOYW1lPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIj4KICAgICAgPEF0dHJpYnV0ZVZhbHVlPmF6cHVic3ViY2xpZW50LXVzZWFzdC5jb3JlLndpbmRvd3MubmV0PC9BdHRyaWJ1dGVWYWx1ZT4KICAgIDwvQXR0cmlidXRlPgogICAgPEF0dHJpYnV0ZSBOYW1lPSJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL3dzLzIwMDgvMDYvaWRlbnRpdHkvY2xhaW1zL3JvbGUiPgogICAgICA8QXR0cmlidXRlVmFsdWU+VG9BdXRoZW50aWNhdGVBelB1YlN1YjwvQXR0cmlidXRlVmFsdWU+CiAgICA8L0F0dHJpYnV0ZT4KICA8L0F0dHJpYnV0ZVN0YXRlbWVudD4KPC9Bc3NlcnRpb24+\"" +
          "}"

        val principal = new KafkaPrincipal(KafkaPrincipal.TOKEN_TYPE, tokenJsonString)
        val localHost = java.net.InetAddress.getLocalHost
        val session = Session(principal, localHost)
        val resource = Resource(Topic, "testTopic")

        val authorizer: AzPubSubAclAuthorizer = EasyMock.partialMockBuilder(classOf[AzPubSubAclAuthorizer])
          .addMockedMethod("getAcls", classOf[Resource])
          .createMock()
        val acls = Set(Acl(new KafkaPrincipal("Role", "ToAuthenticateAzPubSub"), Allow, "*", All))
        EasyMock.expect(authorizer.getAcls(isA(classOf[Resource]))).andReturn(acls).anyTimes()
        suppress(MemberMatcher.methodsDeclaredIn(classOf[KafkaMetricsGroup]))
        suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newGauge"))

        val logger: org.slf4j.Logger = EasyMock.mock(classOf[org.slf4j.Logger])

        Whitebox.setInternalState(authorizer, classOf[org.slf4j.Logger], logger.asInstanceOf[Any])
        EasyMock.expect(logger.info(isA(classOf[String]))).andVoid().anyTimes()
        EasyMock.expect(logger.debug(isA(classOf[String]))).andVoid().anyTimes()
        EasyMock.expect(logger.warn(isA(classOf[String]))).andVoid().anyTimes()

        val cache = new mutable.HashMap[String, Date]
        val validator = new mockPositiveTokenValidator

        Whitebox.setInternalState(authorizer, "cacheTokenLastValidatedTime", cache.asInstanceOf[Any])
        Whitebox.setInternalState(authorizer, "tokenAuthenticator", validator.asInstanceOf[Any])

        EasyMock.replay(authorizer)

        assert(true == authorizer.authorize(session, All, resource))
        EasyMock.verify(authorizer)
      }

      @Test
      def testAzPubSubAclAuthorizerAuthorizeTokenNegative(): Unit = {
            val tokenJsonString = "{" +
              "\"Roles\":" +
              "[" +
              "\"ToAuthenticateAzPubSub\"" +
              "]," +
              "\"ValidFrom\":\"6/18/2019 7:08:11 AM\"," +
              "\"ValidTo\":\"6/19/2079 7:08:11 AM\"," +
              "\"UniqueId\":\"3efcea45-7aae-4fb9-af22-31d6cf9f0b20\"," +
              "\"Base64Token\":" +
              "\"PEFzc2VydGlvbiBJRD0iXzdkYzNmOWU2LWJhOGUtNDQzNS04ZGM2LTNhMTk0YTRiMzFjOSIgSXNzdWVJbnN0YW50PSIyMDE5LTA2LTE4VDA3OjA4OjExLjUzNFoiIFZlcnNpb249IjIuMCIgeG1sbnM9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iPgogIDxJc3N1ZXI+cmVhbG06Ly9zYW1wbGVyZWFsbS5uZXQvPC9Jc3N1ZXI+CiAgPGRzOlNpZ25hdHVyZSB4bWxuczpkcz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI+CiAgICA8ZHM6U2lnbmVkSW5mbz4KICAgICAgPGRzOkNhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiIC8+CiAgICAgIDxkczpTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzA0L3htbGRzaWctbW9yZSNyc2Etc2hhMjU2IiAvPgogICAgICA8ZHM6UmVmZXJlbmNlIFVSST0iI183ZGMzZjllNi1iYThlLTQ0MzUtOGRjNi0zYTE5NGE0YjMxYzkiPgogICAgICAgIDxkczpUcmFuc2Zvcm1zPgogICAgICAgICAgPGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNlbnZlbG9wZWQtc2lnbmF0dXJlIiAvPgogICAgICAgICAgPGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIgLz4KICAgICAgICA8L2RzOlRyYW5zZm9ybXM+CiAgICAgICAgPGRzOkRpZ2VzdE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZW5jI3NoYTI1NiIgLz4KICAgICAgICA8ZHM6RGlnZXN0VmFsdWU+UHV5Z0pWSTBobEI4TDE2YmFsb09MOHJEdHgzQ2RnOS94VkRzYUx2aXo5VT08L2RzOkRpZ2VzdFZhbHVlPgogICAgICA8L2RzOlJlZmVyZW5jZT4KICAgIDwvZHM6U2lnbmVkSW5mbz4KICAgIDxkczpTaWduYXR1cmVWYWx1ZT5IcnFzNm52dkpFMElsNEZvUzBmYUkyc1JFQ3hOdFFLZDRKZ0tVcTIwd21wODJCajU1bEZhY1h3ZndoWUlSTUV2eUNVK2pZVHZZRXNqVVpZUEdCR3JlOENPWkt3Z2FYTDM5R0g1RG53WEpHSURRc3RocCtvM05CeFpFQ09sODVEVWhaZ2k0akhPR1pjRFpFalVXNXM4Z1d4a2FManBlakc2Y3lRUytiaHRBTVBxK1ByS3JPVG1jaEkrTStFM0g5eUpnYys2RlJQdjVldUc4UDZzd2lOb2QzVmFvY2tsdHE2S1VQdXNhbFFtTTllaklxdDVBdmlQb3VwWGx4WHI1RTZ5VENnMGxhR1B5WFhGbEljUWJyYWNQcDBuekl0MkR1RU1TS2wvbnlMS2JISkRKdDRGRGtLYktyb243aityblNqdnFXRmd0cElxT09ZWHF1ZWIxbTNZTUE9PTwvZHM6U2lnbmF0dXJlVmFsdWU+CiAgICA8S2V5SW5mbyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI+CiAgICAgIDxYNTA5RGF0YT4KICAgICAgICA8WDUwOUNlcnRpZmljYXRlPk1JSUpOVENDQngyZ0F3SUJBZ0lUSUFBRVcybUJEdGxDelg0T2t3QUFBQVJiYVRBTkJna3Foa2lHOXcwQkFRc0ZBRENCaXpFTE1Ba0dBMVVFQmhNQ1ZWTXhFekFSQmdOVkJBZ1RDbGRoYzJocGJtZDBiMjR4RURBT0JnTlZCQWNUQjFKbFpHMXZibVF4SGpBY0JnTlZCQW9URlUxcFkzSnZjMjltZENCRGIzSndiM0poZEdsdmJqRVZNQk1HQTFVRUN4TU1UV2xqY205emIyWjBJRWxVTVI0d0hBWURWUVFERXhWTmFXTnliM052Wm5RZ1NWUWdWRXhUSUVOQklESXdIaGNOTVRneE1ERXhNakl6TkRVd1doY05NakF4TURFeE1qSXpORFV3V2pBbE1TTXdJUVlEVlFRRERCb3FMbVJ6ZEhNdVkyOXlaUzVoZW5WeVpTMTBaWE4wTG01bGREQ0NBU0l3RFFZSktvWklodmNOQVFFQkJRQURnZ0VQQURDQ0FRb0NnZ0VCQUozRnU1UDRBQnRxTk53S0YxS0VZSkxGc1hMMWU2cnhxN3ZjM3dEd0pjWHBWUWpRM0IwVHpVd0Zqc0J4S2VOamhwUzlrcFk2Y1pqNVEyWkgvREdzUUZVNEZqK2xtYlJuQXRQWTNXT3NwM3lyWEJUTWFEQkFySkNxYXdCMFBHT0ZPWStwZWdkZnhPR0JTSVp1Q29vWkpOOVB0T3h3OVk4RUgzOC90NnczU0prNzBOb1N2cFhhdU5Gc2lyT3FjNkpTRktJSEN0STdJdDZSTEtnOTM1b3YvY2xrd3NPNFMxTDcvZDIvWnowenBGNWVBNU1xWUU5Wlg3TUU4akZMNU5oWUpjYU1jWXFSZUpTVlc2N2Q2eGt4T1huaCtVL0JBTDE1ZnlYYTJOU0x1S0dnd3JneitYVHJkUi9YUytwQlpoSEplSmYyOWw1ekJvUlNzd2NzenhQY0FMOENBd0VBQWFPQ0JQVXdnZ1R4TUlJQjlRWUtLd1lCQkFIV2VRSUVBZ1NDQWVVRWdnSGhBZDhBZGdDa3VRbVF0QmhZRkllN0U2TE1aM0FLUERXWUJQa2IzN2pqZDgwT3lBM2NFQUFBQVdabFRkazRBQUFFQXdCSE1FVUNJUURIZUo3OTFCYTdKS0dXaWYwa1E1ajNLWFhOU2dhYW5tMXdBWk9BZm84WTJ3SWdGQ245Q0l0bUM0aDY3RmczcTZmaG92U2tSeUpLTmdHSTlVTS9JRXNLNXlrQWRRRHdsYVJaOGdEUmdrQVFMUytUaUk2dFMvNGRSK09aNGRBMHByQ29xbzZ5Y3dBQUFXWmxUZGxLQUFBRUF3QkdNRVFDSUZxQnNUSkMreHZ0Q01oaVRSbkUzRm8vQldtcDZYNDV1TnRNWUsrRmpPTHBBaUFlek1oZVdjaitHem5sd0M4TTcrU2RVUFFTaWJ0aitqbmlpcHFUVGJ0MjlRQjNBRjZuYy9uZlZzRG50VFpJZmRCSjRESjZrWm9NaEtFU0VvUVlkWmFCY1VWWUFBQUJabVZOMlZRQUFBUURBRWd3UmdJaEFMWWpEZ3o3djRSUTFtS2d5SCtuaUpzS05mL2RlMFRqOVFiZ2N5RkxUZEtLQWlFQS8rNWN1bjRyUkxTSXV3ZHR5enowNlJ5M1BEcXhDMDJucHdFUks3VitMb1VBZFFCVmdkVENGcEEyQVVycUM1dFhQRlB3d09RNGVIQWxDQmN2bzZvZEJ4UFREQUFBQVdabFRkcy9BQUFFQXdCR01FUUNJQ1BXUWd2UE9HREpadlRrV2daNmljZjBMdElwN3o4OG9FVmI1MDFOMXdzeUFpQnNGalBTUTAwS0JURjE0aVh1RmNGWW9ObWU1bmFCRGxNRVRyaGhrQ2Zzb3pBbkJna3JCZ0VFQVlJM0ZRb0VHakFZTUFvR0NDc0dBUVVGQndNQ01Bb0dDQ3NHQVFVRkJ3TUJNRDRHQ1NzR0FRUUJnamNWQndReE1DOEdKeXNHQVFRQmdqY1ZDSWZhaG5XRDd0a0Jnc21GRzRHMW5tR0Y5T3RnZ1YyRTB0OUNndWVUZWdJQlpBSUJIVENCaFFZSUt3WUJCUVVIQVFFRWVUQjNNRkVHQ0NzR0FRVUZCekFDaGtWb2RIUndPaTh2ZDNkM0xtMXBZM0p2YzI5bWRDNWpiMjB2Y0d0cEwyMXpZMjl5Y0M5TmFXTnliM052Wm5RbE1qQkpWQ1V5TUZSTVV5VXlNRU5CSlRJd01pNWpjblF3SWdZSUt3WUJCUVVITUFHR0ZtaDBkSEE2THk5dlkzTndMbTF6YjJOemNDNWpiMjB3SFFZRFZSME9CQllFRkVVbnREWTgyQTNTUTc0SkJBNlRua2tLSS9DZE1Bc0dBMVVkRHdRRUF3SUVzRENCbWdZRFZSMFJCSUdTTUlHUGdob3FMbVJ6ZEhNdVkyOXlaUzVoZW5WeVpTMTBaWE4wTG01bGRJSWJLaTVrYzNSekxtTnZjbVV1ZDJsdVpHOTNjeTFwYm5RdWJtVjBnaHNxTG1SemRITXVZMjl5WlM1M2FXNWtiM2R6TFhSemRDNXVaWFNDSFNvdVpITjBjeTVsTW1WMFpYTjBNaTVoZW5WeVpTMXBiblF1Ym1WMGdoZ3FMbVJ6ZEhNdWFXNTBMbUY2ZFhKbExXbHVkQzV1WlhRd2dhd0dBMVVkSHdTQnBEQ0JvVENCbnFDQm02Q0JtSVpMYUhSMGNEb3ZMMjF6WTNKc0xtMXBZM0p2YzI5bWRDNWpiMjB2Y0d0cEwyMXpZMjl5Y0M5amNtd3ZUV2xqY205emIyWjBKVEl3U1ZRbE1qQlVURk1sTWpCRFFTVXlNREl1WTNKc2hrbG9kSFJ3T2k4dlkzSnNMbTFwWTNKdmMyOW1kQzVqYjIwdmNHdHBMMjF6WTI5eWNDOWpjbXd2VFdsamNtOXpiMlowSlRJd1NWUWxNakJVVEZNbE1qQkRRU1V5TURJdVkzSnNNRTBHQTFVZElBUkdNRVF3UWdZSkt3WUJCQUdDTnlvQk1EVXdNd1lJS3dZQkJRVUhBZ0VXSjJoMGRIQTZMeTkzZDNjdWJXbGpjbTl6YjJaMExtTnZiUzl3YTJrdmJYTmpiM0p3TDJOd2N6QWZCZ05WSFNNRUdEQVdnQlNSbmp0RWJEMVhuRUozS2pUWFQ5SE1TcGNzMmpBZEJnTlZIU1VFRmpBVUJnZ3JCZ0VGQlFjREFnWUlLd1lCQlFVSEF3RXdEUVlKS29aSWh2Y05BUUVMQlFBRGdnSUJBRTAvMGU0bmVxOHc0dE5WR3pzam5LUW1tVm5FMGlnUmtxMVNTbkk0TDJXenBKN1V0a2J1RWZmZ1BKRVpHZkt5aGRzVVVFR1VlZ0NMWkVmbWk0d2ZoV21tdWcxZ2dVOFdLbVZ5NTEzejUyY1h6YkpQM3NNYXNaZno0UkJ1YlZKdWlMUzJ1VG5EMkpObGV2cEwzdkJHR1Z2T2lCb1gyakhDbDJGNUdNV0wvZTdKaUtKOWtNenNENWpidktBNFJDQ0h6NVFmL0tnU0Z6ZU1qSHFQSnFCQmV4bk0wQ3dJWlBhaCtMYWcyb1RLZGttZlFpZnRzSjZwNnE3ZVI5ejRRMkl4U1JQbGhmRThLWkMxZExCd1NOMUgySFhkWDZ0SW1QSVBtNnd3eGd1eVJab0JzSC8xRTY3N0h1VWFKUEE0eFdOTkcwaXdOR044TnNLek5OMEdkMHN4OW56YkhsYlFEZEJLVFlIMUNDRk16TmNTdUJqaU1hOFQ3c3pTVStoeDFwRmplYWlYTFJ6d1pqdUlkeDQ1MnQ4b0RKY0VZNEI3KzZEQm9uTGs4TVlTNGZsUm5wcGZabnB3TERNcVVVZFRNNkJjRExGOXNhdlV1dzhaOG9OUm1jQjEwTlB3ZEplYzBzRWRoSDNNRThvVkFoWjZ2dHRFa2lLQmhPZXZjY2w4Umh1ekE1YnVjZFc2M0RkQVFPWDJGUzFoNG0vRGxIeU9PMFA3NG5WdTkzQUlnc0hPVGsvbFlxWnZXUVh5dmg4NHk4U1FQRm1wejBXOHduT1pIMUdXTXRXMkt2em8vU2phb2ZoU0NYa0FWa3hBN1dGU2llL2pCUlRxVzc0WWsxQTRIRHpCaml6UlZJM0FKRCtiWldjMGNFSlV0NHBOQ3pVQmY3M2crU0pKOWErUDVvbkI8L1g1MDlDZXJ0aWZpY2F0ZT4KICAgICAgPC9YNTA5RGF0YT4KICAgIDwvS2V5SW5mbz4KICA8L2RzOlNpZ25hdHVyZT4KICA8U3ViamVjdD4KICAgIDxOYW1lSUQ+c2FtcGxlLnRlc3QuYXp1cmUubmV0PC9OYW1lSUQ+CiAgICA8U3ViamVjdENvbmZpcm1hdGlvbiBNZXRob2Q9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpjbTpiZWFyZXIiIC8+CiAgPC9TdWJqZWN0PgogIDxDb25kaXRpb25zIE5vdEJlZm9yZT0iMjAxOS0wNi0xOFQwNzowODoxMS41MThaIiBOb3RPbk9yQWZ0ZXI9IjIwNzktMDYtMTlUMDc6MDg6MTEuNTE4WiI+CiAgICA8QXVkaWVuY2VSZXN0cmljdGlvbj4KICAgICAgPEF1ZGllbmNlPnN2YzovL0F6UHViU3ViU2VydmljZVRlc3RAc2FtcGxlLWRzdHMtYXdhcmUtc2VydmljZS5jb3JlLndpbmRvd3MtdHN0Lm5ldC88L0F1ZGllbmNlPgogICAgPC9BdWRpZW5jZVJlc3RyaWN0aW9uPgogIDwvQ29uZGl0aW9ucz4KICA8QXR0cmlidXRlU3RhdGVtZW50PgogICAgPEF0dHJpYnV0ZSBOYW1lPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIj4KICAgICAgPEF0dHJpYnV0ZVZhbHVlPmF6cHVic3ViY2xpZW50LXVzZWFzdC5jb3JlLndpbmRvd3MubmV0PC9BdHRyaWJ1dGVWYWx1ZT4KICAgIDwvQXR0cmlidXRlPgogICAgPEF0dHJpYnV0ZSBOYW1lPSJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL3dzLzIwMDgvMDYvaWRlbnRpdHkvY2xhaW1zL3JvbGUiPgogICAgICA8QXR0cmlidXRlVmFsdWU+VG9BdXRoZW50aWNhdGVBelB1YlN1YjwvQXR0cmlidXRlVmFsdWU+CiAgICA8L0F0dHJpYnV0ZT4KICA8L0F0dHJpYnV0ZVN0YXRlbWVudD4KPC9Bc3NlcnRpb24+\"" +
              "}"

            val principal = new KafkaPrincipal(KafkaPrincipal.TOKEN_TYPE, tokenJsonString)
            val localHost = java.net.InetAddress.getLocalHost
            val session = Session(principal, localHost)
            val resource = Resource(Topic, "testTopic")

            val authorizer: AzPubSubAclAuthorizer = EasyMock.partialMockBuilder(classOf[AzPubSubAclAuthorizer])
              .addMockedMethod("getAcls", classOf[Resource])
              .createMock()
            val acls = Set(Acl(new KafkaPrincipal("Role", "ToAuthenticateAzPubSub"), Allow, "*", All))
            EasyMock.expect(authorizer.getAcls(isA(classOf[Resource]))).andReturn(acls).anyTimes()
            suppress(MemberMatcher.methodsDeclaredIn(classOf[KafkaMetricsGroup]))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newGauge"))

            val logger: org.slf4j.Logger = EasyMock.mock(classOf[org.slf4j.Logger])

            Whitebox.setInternalState(authorizer, classOf[org.slf4j.Logger], logger.asInstanceOf[Any])
            EasyMock.expect(logger.info(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.debug(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.warn(isA(classOf[String]))).andVoid().anyTimes()

            val cache = new mutable.HashMap[String, Date]
            val validator = new mockNegativeTokenValidator

            Whitebox.setInternalState(authorizer, "cacheTokenLastValidatedTime", cache.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "tokenAuthenticator", validator.asInstanceOf[Any])

            EasyMock.replay(authorizer)

            assert(false == authorizer.authorize(session, All, resource))
            EasyMock.verify(authorizer)
      }

      @Test
      def testAzPubSubAclAuthorizerInvalidFromDate(): Unit = {
            val tokenJsonString = "{" +
              "\"Roles\":" +
              "[" +
              "\"ToAuthenticateAzPubSub\"" +
              "]," +
              "\"ValidFrom\":\"6/18/2079 7:08:11 AM\"," +
              "\"ValidTo\":\"6/19/2199 7:08:11 AM\"," +
              "\"UniqueId\":\"3efcea45-7aae-4fb9-af22-31d6cf9f0b20\"," +
              "\"Base64Token\":" +
              "\"PEFzc2VydGlvbiBJRD0iXzdkYzNmOWU2LWJhOGUtNDQzNS04ZGM2LTNhMTk0YTRiMzFjOSIgSXNzdWVJbnN0YW50PSIyMDE5LTA2LTE4VDA3OjA4OjExLjUzNFoiIFZlcnNpb249IjIuMCIgeG1sbnM9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iPgogIDxJc3N1ZXI+cmVhbG06Ly9zYW1wbGVyZWFsbS5uZXQvPC9Jc3N1ZXI+CiAgPGRzOlNpZ25hdHVyZSB4bWxuczpkcz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI+CiAgICA8ZHM6U2lnbmVkSW5mbz4KICAgICAgPGRzOkNhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiIC8+CiAgICAgIDxkczpTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzA0L3htbGRzaWctbW9yZSNyc2Etc2hhMjU2IiAvPgogICAgICA8ZHM6UmVmZXJlbmNlIFVSST0iI183ZGMzZjllNi1iYThlLTQ0MzUtOGRjNi0zYTE5NGE0YjMxYzkiPgogICAgICAgIDxkczpUcmFuc2Zvcm1zPgogICAgICAgICAgPGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNlbnZlbG9wZWQtc2lnbmF0dXJlIiAvPgogICAgICAgICAgPGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIgLz4KICAgICAgICA8L2RzOlRyYW5zZm9ybXM+CiAgICAgICAgPGRzOkRpZ2VzdE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZW5jI3NoYTI1NiIgLz4KICAgICAgICA8ZHM6RGlnZXN0VmFsdWU+UHV5Z0pWSTBobEI4TDE2YmFsb09MOHJEdHgzQ2RnOS94VkRzYUx2aXo5VT08L2RzOkRpZ2VzdFZhbHVlPgogICAgICA8L2RzOlJlZmVyZW5jZT4KICAgIDwvZHM6U2lnbmVkSW5mbz4KICAgIDxkczpTaWduYXR1cmVWYWx1ZT5IcnFzNm52dkpFMElsNEZvUzBmYUkyc1JFQ3hOdFFLZDRKZ0tVcTIwd21wODJCajU1bEZhY1h3ZndoWUlSTUV2eUNVK2pZVHZZRXNqVVpZUEdCR3JlOENPWkt3Z2FYTDM5R0g1RG53WEpHSURRc3RocCtvM05CeFpFQ09sODVEVWhaZ2k0akhPR1pjRFpFalVXNXM4Z1d4a2FManBlakc2Y3lRUytiaHRBTVBxK1ByS3JPVG1jaEkrTStFM0g5eUpnYys2RlJQdjVldUc4UDZzd2lOb2QzVmFvY2tsdHE2S1VQdXNhbFFtTTllaklxdDVBdmlQb3VwWGx4WHI1RTZ5VENnMGxhR1B5WFhGbEljUWJyYWNQcDBuekl0MkR1RU1TS2wvbnlMS2JISkRKdDRGRGtLYktyb243aityblNqdnFXRmd0cElxT09ZWHF1ZWIxbTNZTUE9PTwvZHM6U2lnbmF0dXJlVmFsdWU+CiAgICA8S2V5SW5mbyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI+CiAgICAgIDxYNTA5RGF0YT4KICAgICAgICA8WDUwOUNlcnRpZmljYXRlPk1JSUpOVENDQngyZ0F3SUJBZ0lUSUFBRVcybUJEdGxDelg0T2t3QUFBQVJiYVRBTkJna3Foa2lHOXcwQkFRc0ZBRENCaXpFTE1Ba0dBMVVFQmhNQ1ZWTXhFekFSQmdOVkJBZ1RDbGRoYzJocGJtZDBiMjR4RURBT0JnTlZCQWNUQjFKbFpHMXZibVF4SGpBY0JnTlZCQW9URlUxcFkzSnZjMjltZENCRGIzSndiM0poZEdsdmJqRVZNQk1HQTFVRUN4TU1UV2xqY205emIyWjBJRWxVTVI0d0hBWURWUVFERXhWTmFXTnliM052Wm5RZ1NWUWdWRXhUSUVOQklESXdIaGNOTVRneE1ERXhNakl6TkRVd1doY05NakF4TURFeE1qSXpORFV3V2pBbE1TTXdJUVlEVlFRRERCb3FMbVJ6ZEhNdVkyOXlaUzVoZW5WeVpTMTBaWE4wTG01bGREQ0NBU0l3RFFZSktvWklodmNOQVFFQkJRQURnZ0VQQURDQ0FRb0NnZ0VCQUozRnU1UDRBQnRxTk53S0YxS0VZSkxGc1hMMWU2cnhxN3ZjM3dEd0pjWHBWUWpRM0IwVHpVd0Zqc0J4S2VOamhwUzlrcFk2Y1pqNVEyWkgvREdzUUZVNEZqK2xtYlJuQXRQWTNXT3NwM3lyWEJUTWFEQkFySkNxYXdCMFBHT0ZPWStwZWdkZnhPR0JTSVp1Q29vWkpOOVB0T3h3OVk4RUgzOC90NnczU0prNzBOb1N2cFhhdU5Gc2lyT3FjNkpTRktJSEN0STdJdDZSTEtnOTM1b3YvY2xrd3NPNFMxTDcvZDIvWnowenBGNWVBNU1xWUU5Wlg3TUU4akZMNU5oWUpjYU1jWXFSZUpTVlc2N2Q2eGt4T1huaCtVL0JBTDE1ZnlYYTJOU0x1S0dnd3JneitYVHJkUi9YUytwQlpoSEplSmYyOWw1ekJvUlNzd2NzenhQY0FMOENBd0VBQWFPQ0JQVXdnZ1R4TUlJQjlRWUtLd1lCQkFIV2VRSUVBZ1NDQWVVRWdnSGhBZDhBZGdDa3VRbVF0QmhZRkllN0U2TE1aM0FLUERXWUJQa2IzN2pqZDgwT3lBM2NFQUFBQVdabFRkazRBQUFFQXdCSE1FVUNJUURIZUo3OTFCYTdKS0dXaWYwa1E1ajNLWFhOU2dhYW5tMXdBWk9BZm84WTJ3SWdGQ245Q0l0bUM0aDY3RmczcTZmaG92U2tSeUpLTmdHSTlVTS9JRXNLNXlrQWRRRHdsYVJaOGdEUmdrQVFMUytUaUk2dFMvNGRSK09aNGRBMHByQ29xbzZ5Y3dBQUFXWmxUZGxLQUFBRUF3QkdNRVFDSUZxQnNUSkMreHZ0Q01oaVRSbkUzRm8vQldtcDZYNDV1TnRNWUsrRmpPTHBBaUFlek1oZVdjaitHem5sd0M4TTcrU2RVUFFTaWJ0aitqbmlpcHFUVGJ0MjlRQjNBRjZuYy9uZlZzRG50VFpJZmRCSjRESjZrWm9NaEtFU0VvUVlkWmFCY1VWWUFBQUJabVZOMlZRQUFBUURBRWd3UmdJaEFMWWpEZ3o3djRSUTFtS2d5SCtuaUpzS05mL2RlMFRqOVFiZ2N5RkxUZEtLQWlFQS8rNWN1bjRyUkxTSXV3ZHR5enowNlJ5M1BEcXhDMDJucHdFUks3VitMb1VBZFFCVmdkVENGcEEyQVVycUM1dFhQRlB3d09RNGVIQWxDQmN2bzZvZEJ4UFREQUFBQVdabFRkcy9BQUFFQXdCR01FUUNJQ1BXUWd2UE9HREpadlRrV2daNmljZjBMdElwN3o4OG9FVmI1MDFOMXdzeUFpQnNGalBTUTAwS0JURjE0aVh1RmNGWW9ObWU1bmFCRGxNRVRyaGhrQ2Zzb3pBbkJna3JCZ0VFQVlJM0ZRb0VHakFZTUFvR0NDc0dBUVVGQndNQ01Bb0dDQ3NHQVFVRkJ3TUJNRDRHQ1NzR0FRUUJnamNWQndReE1DOEdKeXNHQVFRQmdqY1ZDSWZhaG5XRDd0a0Jnc21GRzRHMW5tR0Y5T3RnZ1YyRTB0OUNndWVUZWdJQlpBSUJIVENCaFFZSUt3WUJCUVVIQVFFRWVUQjNNRkVHQ0NzR0FRVUZCekFDaGtWb2RIUndPaTh2ZDNkM0xtMXBZM0p2YzI5bWRDNWpiMjB2Y0d0cEwyMXpZMjl5Y0M5TmFXTnliM052Wm5RbE1qQkpWQ1V5TUZSTVV5VXlNRU5CSlRJd01pNWpjblF3SWdZSUt3WUJCUVVITUFHR0ZtaDBkSEE2THk5dlkzTndMbTF6YjJOemNDNWpiMjB3SFFZRFZSME9CQllFRkVVbnREWTgyQTNTUTc0SkJBNlRua2tLSS9DZE1Bc0dBMVVkRHdRRUF3SUVzRENCbWdZRFZSMFJCSUdTTUlHUGdob3FMbVJ6ZEhNdVkyOXlaUzVoZW5WeVpTMTBaWE4wTG01bGRJSWJLaTVrYzNSekxtTnZjbVV1ZDJsdVpHOTNjeTFwYm5RdWJtVjBnaHNxTG1SemRITXVZMjl5WlM1M2FXNWtiM2R6TFhSemRDNXVaWFNDSFNvdVpITjBjeTVsTW1WMFpYTjBNaTVoZW5WeVpTMXBiblF1Ym1WMGdoZ3FMbVJ6ZEhNdWFXNTBMbUY2ZFhKbExXbHVkQzV1WlhRd2dhd0dBMVVkSHdTQnBEQ0JvVENCbnFDQm02Q0JtSVpMYUhSMGNEb3ZMMjF6WTNKc0xtMXBZM0p2YzI5bWRDNWpiMjB2Y0d0cEwyMXpZMjl5Y0M5amNtd3ZUV2xqY205emIyWjBKVEl3U1ZRbE1qQlVURk1sTWpCRFFTVXlNREl1WTNKc2hrbG9kSFJ3T2k4dlkzSnNMbTFwWTNKdmMyOW1kQzVqYjIwdmNHdHBMMjF6WTI5eWNDOWpjbXd2VFdsamNtOXpiMlowSlRJd1NWUWxNakJVVEZNbE1qQkRRU1V5TURJdVkzSnNNRTBHQTFVZElBUkdNRVF3UWdZSkt3WUJCQUdDTnlvQk1EVXdNd1lJS3dZQkJRVUhBZ0VXSjJoMGRIQTZMeTkzZDNjdWJXbGpjbTl6YjJaMExtTnZiUzl3YTJrdmJYTmpiM0p3TDJOd2N6QWZCZ05WSFNNRUdEQVdnQlNSbmp0RWJEMVhuRUozS2pUWFQ5SE1TcGNzMmpBZEJnTlZIU1VFRmpBVUJnZ3JCZ0VGQlFjREFnWUlLd1lCQlFVSEF3RXdEUVlKS29aSWh2Y05BUUVMQlFBRGdnSUJBRTAvMGU0bmVxOHc0dE5WR3pzam5LUW1tVm5FMGlnUmtxMVNTbkk0TDJXenBKN1V0a2J1RWZmZ1BKRVpHZkt5aGRzVVVFR1VlZ0NMWkVmbWk0d2ZoV21tdWcxZ2dVOFdLbVZ5NTEzejUyY1h6YkpQM3NNYXNaZno0UkJ1YlZKdWlMUzJ1VG5EMkpObGV2cEwzdkJHR1Z2T2lCb1gyakhDbDJGNUdNV0wvZTdKaUtKOWtNenNENWpidktBNFJDQ0h6NVFmL0tnU0Z6ZU1qSHFQSnFCQmV4bk0wQ3dJWlBhaCtMYWcyb1RLZGttZlFpZnRzSjZwNnE3ZVI5ejRRMkl4U1JQbGhmRThLWkMxZExCd1NOMUgySFhkWDZ0SW1QSVBtNnd3eGd1eVJab0JzSC8xRTY3N0h1VWFKUEE0eFdOTkcwaXdOR044TnNLek5OMEdkMHN4OW56YkhsYlFEZEJLVFlIMUNDRk16TmNTdUJqaU1hOFQ3c3pTVStoeDFwRmplYWlYTFJ6d1pqdUlkeDQ1MnQ4b0RKY0VZNEI3KzZEQm9uTGs4TVlTNGZsUm5wcGZabnB3TERNcVVVZFRNNkJjRExGOXNhdlV1dzhaOG9OUm1jQjEwTlB3ZEplYzBzRWRoSDNNRThvVkFoWjZ2dHRFa2lLQmhPZXZjY2w4Umh1ekE1YnVjZFc2M0RkQVFPWDJGUzFoNG0vRGxIeU9PMFA3NG5WdTkzQUlnc0hPVGsvbFlxWnZXUVh5dmg4NHk4U1FQRm1wejBXOHduT1pIMUdXTXRXMkt2em8vU2phb2ZoU0NYa0FWa3hBN1dGU2llL2pCUlRxVzc0WWsxQTRIRHpCaml6UlZJM0FKRCtiWldjMGNFSlV0NHBOQ3pVQmY3M2crU0pKOWErUDVvbkI8L1g1MDlDZXJ0aWZpY2F0ZT4KICAgICAgPC9YNTA5RGF0YT4KICAgIDwvS2V5SW5mbz4KICA8L2RzOlNpZ25hdHVyZT4KICA8U3ViamVjdD4KICAgIDxOYW1lSUQ+c2FtcGxlLnRlc3QuYXp1cmUubmV0PC9OYW1lSUQ+CiAgICA8U3ViamVjdENvbmZpcm1hdGlvbiBNZXRob2Q9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpjbTpiZWFyZXIiIC8+CiAgPC9TdWJqZWN0PgogIDxDb25kaXRpb25zIE5vdEJlZm9yZT0iMjAxOS0wNi0xOFQwNzowODoxMS41MThaIiBOb3RPbk9yQWZ0ZXI9IjIwNzktMDYtMTlUMDc6MDg6MTEuNTE4WiI+CiAgICA8QXVkaWVuY2VSZXN0cmljdGlvbj4KICAgICAgPEF1ZGllbmNlPnN2YzovL0F6UHViU3ViU2VydmljZVRlc3RAc2FtcGxlLWRzdHMtYXdhcmUtc2VydmljZS5jb3JlLndpbmRvd3MtdHN0Lm5ldC88L0F1ZGllbmNlPgogICAgPC9BdWRpZW5jZVJlc3RyaWN0aW9uPgogIDwvQ29uZGl0aW9ucz4KICA8QXR0cmlidXRlU3RhdGVtZW50PgogICAgPEF0dHJpYnV0ZSBOYW1lPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIj4KICAgICAgPEF0dHJpYnV0ZVZhbHVlPmF6cHVic3ViY2xpZW50LXVzZWFzdC5jb3JlLndpbmRvd3MubmV0PC9BdHRyaWJ1dGVWYWx1ZT4KICAgIDwvQXR0cmlidXRlPgogICAgPEF0dHJpYnV0ZSBOYW1lPSJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL3dzLzIwMDgvMDYvaWRlbnRpdHkvY2xhaW1zL3JvbGUiPgogICAgICA8QXR0cmlidXRlVmFsdWU+VG9BdXRoZW50aWNhdGVBelB1YlN1YjwvQXR0cmlidXRlVmFsdWU+CiAgICA8L0F0dHJpYnV0ZT4KICA8L0F0dHJpYnV0ZVN0YXRlbWVudD4KPC9Bc3NlcnRpb24+\"" +
              "}"

            val principal = new KafkaPrincipal(KafkaPrincipal.TOKEN_TYPE, tokenJsonString)
            val localHost = java.net.InetAddress.getLocalHost
            val session = Session(principal, localHost)
            val resource = Resource(Topic, "testTopic")

            val authorizer: AzPubSubAclAuthorizer = EasyMock.partialMockBuilder(classOf[AzPubSubAclAuthorizer])
              .addMockedMethod("getAcls", classOf[Resource])
              .createMock()
            val acls = Set(Acl(new KafkaPrincipal("Role", "ToAuthenticateAzPubSub"), Allow, "*", All))
            EasyMock.expect(authorizer.getAcls(isA(classOf[Resource]))).andReturn(acls).anyTimes()
            suppress(MemberMatcher.methodsDeclaredIn(classOf[KafkaMetricsGroup]))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newGauge"))

            val logger: org.slf4j.Logger = EasyMock.mock(classOf[org.slf4j.Logger])

            Whitebox.setInternalState(authorizer, classOf[org.slf4j.Logger], logger.asInstanceOf[Any])
            EasyMock.expect(logger.info(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.debug(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.warn(isA(classOf[String]))).andVoid().anyTimes()

            val cache = new mutable.HashMap[String, Date]
            val validator = new mockPositiveTokenValidator

            Whitebox.setInternalState(authorizer, "cacheTokenLastValidatedTime", cache.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "tokenAuthenticator", validator.asInstanceOf[Any])

            EasyMock.replay(authorizer)

            assert(false == authorizer.authorize(session, All, resource))
            EasyMock.verify(authorizer)
      }

      @Test
      def testAzPubSubAclAuthorizerInvalidEndDate(): Unit = {
            val tokenJsonString = "{" +
              "\"Roles\":" +
              "[" +
              "\"ToAuthenticateAzPubSub\"" +
              "]," +
              "\"ValidFrom\":\"6/17/2019 7:08:11 AM\"," +
              "\"ValidTo\":\"6/18/2019 7:08:11 AM\"," +
              "\"UniqueId\":\"3efcea45-7aae-4fb9-af22-31d6cf9f0b20\"," +
              "\"Base64Token\":" +
              "\"PEFzc2VydGlvbiBJRD0iXzdkYzNmOWU2LWJhOGUtNDQzNS04ZGM2LTNhMTk0YTRiMzFjOSIgSXNzdWVJbnN0YW50PSIyMDE5LTA2LTE4VDA3OjA4OjExLjUzNFoiIFZlcnNpb249IjIuMCIgeG1sbnM9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iPgogIDxJc3N1ZXI+cmVhbG06Ly9zYW1wbGVyZWFsbS5uZXQvPC9Jc3N1ZXI+CiAgPGRzOlNpZ25hdHVyZSB4bWxuczpkcz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI+CiAgICA8ZHM6U2lnbmVkSW5mbz4KICAgICAgPGRzOkNhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiIC8+CiAgICAgIDxkczpTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzA0L3htbGRzaWctbW9yZSNyc2Etc2hhMjU2IiAvPgogICAgICA8ZHM6UmVmZXJlbmNlIFVSST0iI183ZGMzZjllNi1iYThlLTQ0MzUtOGRjNi0zYTE5NGE0YjMxYzkiPgogICAgICAgIDxkczpUcmFuc2Zvcm1zPgogICAgICAgICAgPGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNlbnZlbG9wZWQtc2lnbmF0dXJlIiAvPgogICAgICAgICAgPGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIgLz4KICAgICAgICA8L2RzOlRyYW5zZm9ybXM+CiAgICAgICAgPGRzOkRpZ2VzdE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZW5jI3NoYTI1NiIgLz4KICAgICAgICA8ZHM6RGlnZXN0VmFsdWU+UHV5Z0pWSTBobEI4TDE2YmFsb09MOHJEdHgzQ2RnOS94VkRzYUx2aXo5VT08L2RzOkRpZ2VzdFZhbHVlPgogICAgICA8L2RzOlJlZmVyZW5jZT4KICAgIDwvZHM6U2lnbmVkSW5mbz4KICAgIDxkczpTaWduYXR1cmVWYWx1ZT5IcnFzNm52dkpFMElsNEZvUzBmYUkyc1JFQ3hOdFFLZDRKZ0tVcTIwd21wODJCajU1bEZhY1h3ZndoWUlSTUV2eUNVK2pZVHZZRXNqVVpZUEdCR3JlOENPWkt3Z2FYTDM5R0g1RG53WEpHSURRc3RocCtvM05CeFpFQ09sODVEVWhaZ2k0akhPR1pjRFpFalVXNXM4Z1d4a2FManBlakc2Y3lRUytiaHRBTVBxK1ByS3JPVG1jaEkrTStFM0g5eUpnYys2RlJQdjVldUc4UDZzd2lOb2QzVmFvY2tsdHE2S1VQdXNhbFFtTTllaklxdDVBdmlQb3VwWGx4WHI1RTZ5VENnMGxhR1B5WFhGbEljUWJyYWNQcDBuekl0MkR1RU1TS2wvbnlMS2JISkRKdDRGRGtLYktyb243aityblNqdnFXRmd0cElxT09ZWHF1ZWIxbTNZTUE9PTwvZHM6U2lnbmF0dXJlVmFsdWU+CiAgICA8S2V5SW5mbyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI+CiAgICAgIDxYNTA5RGF0YT4KICAgICAgICA8WDUwOUNlcnRpZmljYXRlPk1JSUpOVENDQngyZ0F3SUJBZ0lUSUFBRVcybUJEdGxDelg0T2t3QUFBQVJiYVRBTkJna3Foa2lHOXcwQkFRc0ZBRENCaXpFTE1Ba0dBMVVFQmhNQ1ZWTXhFekFSQmdOVkJBZ1RDbGRoYzJocGJtZDBiMjR4RURBT0JnTlZCQWNUQjFKbFpHMXZibVF4SGpBY0JnTlZCQW9URlUxcFkzSnZjMjltZENCRGIzSndiM0poZEdsdmJqRVZNQk1HQTFVRUN4TU1UV2xqY205emIyWjBJRWxVTVI0d0hBWURWUVFERXhWTmFXTnliM052Wm5RZ1NWUWdWRXhUSUVOQklESXdIaGNOTVRneE1ERXhNakl6TkRVd1doY05NakF4TURFeE1qSXpORFV3V2pBbE1TTXdJUVlEVlFRRERCb3FMbVJ6ZEhNdVkyOXlaUzVoZW5WeVpTMTBaWE4wTG01bGREQ0NBU0l3RFFZSktvWklodmNOQVFFQkJRQURnZ0VQQURDQ0FRb0NnZ0VCQUozRnU1UDRBQnRxTk53S0YxS0VZSkxGc1hMMWU2cnhxN3ZjM3dEd0pjWHBWUWpRM0IwVHpVd0Zqc0J4S2VOamhwUzlrcFk2Y1pqNVEyWkgvREdzUUZVNEZqK2xtYlJuQXRQWTNXT3NwM3lyWEJUTWFEQkFySkNxYXdCMFBHT0ZPWStwZWdkZnhPR0JTSVp1Q29vWkpOOVB0T3h3OVk4RUgzOC90NnczU0prNzBOb1N2cFhhdU5Gc2lyT3FjNkpTRktJSEN0STdJdDZSTEtnOTM1b3YvY2xrd3NPNFMxTDcvZDIvWnowenBGNWVBNU1xWUU5Wlg3TUU4akZMNU5oWUpjYU1jWXFSZUpTVlc2N2Q2eGt4T1huaCtVL0JBTDE1ZnlYYTJOU0x1S0dnd3JneitYVHJkUi9YUytwQlpoSEplSmYyOWw1ekJvUlNzd2NzenhQY0FMOENBd0VBQWFPQ0JQVXdnZ1R4TUlJQjlRWUtLd1lCQkFIV2VRSUVBZ1NDQWVVRWdnSGhBZDhBZGdDa3VRbVF0QmhZRkllN0U2TE1aM0FLUERXWUJQa2IzN2pqZDgwT3lBM2NFQUFBQVdabFRkazRBQUFFQXdCSE1FVUNJUURIZUo3OTFCYTdKS0dXaWYwa1E1ajNLWFhOU2dhYW5tMXdBWk9BZm84WTJ3SWdGQ245Q0l0bUM0aDY3RmczcTZmaG92U2tSeUpLTmdHSTlVTS9JRXNLNXlrQWRRRHdsYVJaOGdEUmdrQVFMUytUaUk2dFMvNGRSK09aNGRBMHByQ29xbzZ5Y3dBQUFXWmxUZGxLQUFBRUF3QkdNRVFDSUZxQnNUSkMreHZ0Q01oaVRSbkUzRm8vQldtcDZYNDV1TnRNWUsrRmpPTHBBaUFlek1oZVdjaitHem5sd0M4TTcrU2RVUFFTaWJ0aitqbmlpcHFUVGJ0MjlRQjNBRjZuYy9uZlZzRG50VFpJZmRCSjRESjZrWm9NaEtFU0VvUVlkWmFCY1VWWUFBQUJabVZOMlZRQUFBUURBRWd3UmdJaEFMWWpEZ3o3djRSUTFtS2d5SCtuaUpzS05mL2RlMFRqOVFiZ2N5RkxUZEtLQWlFQS8rNWN1bjRyUkxTSXV3ZHR5enowNlJ5M1BEcXhDMDJucHdFUks3VitMb1VBZFFCVmdkVENGcEEyQVVycUM1dFhQRlB3d09RNGVIQWxDQmN2bzZvZEJ4UFREQUFBQVdabFRkcy9BQUFFQXdCR01FUUNJQ1BXUWd2UE9HREpadlRrV2daNmljZjBMdElwN3o4OG9FVmI1MDFOMXdzeUFpQnNGalBTUTAwS0JURjE0aVh1RmNGWW9ObWU1bmFCRGxNRVRyaGhrQ2Zzb3pBbkJna3JCZ0VFQVlJM0ZRb0VHakFZTUFvR0NDc0dBUVVGQndNQ01Bb0dDQ3NHQVFVRkJ3TUJNRDRHQ1NzR0FRUUJnamNWQndReE1DOEdKeXNHQVFRQmdqY1ZDSWZhaG5XRDd0a0Jnc21GRzRHMW5tR0Y5T3RnZ1YyRTB0OUNndWVUZWdJQlpBSUJIVENCaFFZSUt3WUJCUVVIQVFFRWVUQjNNRkVHQ0NzR0FRVUZCekFDaGtWb2RIUndPaTh2ZDNkM0xtMXBZM0p2YzI5bWRDNWpiMjB2Y0d0cEwyMXpZMjl5Y0M5TmFXTnliM052Wm5RbE1qQkpWQ1V5TUZSTVV5VXlNRU5CSlRJd01pNWpjblF3SWdZSUt3WUJCUVVITUFHR0ZtaDBkSEE2THk5dlkzTndMbTF6YjJOemNDNWpiMjB3SFFZRFZSME9CQllFRkVVbnREWTgyQTNTUTc0SkJBNlRua2tLSS9DZE1Bc0dBMVVkRHdRRUF3SUVzRENCbWdZRFZSMFJCSUdTTUlHUGdob3FMbVJ6ZEhNdVkyOXlaUzVoZW5WeVpTMTBaWE4wTG01bGRJSWJLaTVrYzNSekxtTnZjbVV1ZDJsdVpHOTNjeTFwYm5RdWJtVjBnaHNxTG1SemRITXVZMjl5WlM1M2FXNWtiM2R6TFhSemRDNXVaWFNDSFNvdVpITjBjeTVsTW1WMFpYTjBNaTVoZW5WeVpTMXBiblF1Ym1WMGdoZ3FMbVJ6ZEhNdWFXNTBMbUY2ZFhKbExXbHVkQzV1WlhRd2dhd0dBMVVkSHdTQnBEQ0JvVENCbnFDQm02Q0JtSVpMYUhSMGNEb3ZMMjF6WTNKc0xtMXBZM0p2YzI5bWRDNWpiMjB2Y0d0cEwyMXpZMjl5Y0M5amNtd3ZUV2xqY205emIyWjBKVEl3U1ZRbE1qQlVURk1sTWpCRFFTVXlNREl1WTNKc2hrbG9kSFJ3T2k4dlkzSnNMbTFwWTNKdmMyOW1kQzVqYjIwdmNHdHBMMjF6WTI5eWNDOWpjbXd2VFdsamNtOXpiMlowSlRJd1NWUWxNakJVVEZNbE1qQkRRU1V5TURJdVkzSnNNRTBHQTFVZElBUkdNRVF3UWdZSkt3WUJCQUdDTnlvQk1EVXdNd1lJS3dZQkJRVUhBZ0VXSjJoMGRIQTZMeTkzZDNjdWJXbGpjbTl6YjJaMExtTnZiUzl3YTJrdmJYTmpiM0p3TDJOd2N6QWZCZ05WSFNNRUdEQVdnQlNSbmp0RWJEMVhuRUozS2pUWFQ5SE1TcGNzMmpBZEJnTlZIU1VFRmpBVUJnZ3JCZ0VGQlFjREFnWUlLd1lCQlFVSEF3RXdEUVlKS29aSWh2Y05BUUVMQlFBRGdnSUJBRTAvMGU0bmVxOHc0dE5WR3pzam5LUW1tVm5FMGlnUmtxMVNTbkk0TDJXenBKN1V0a2J1RWZmZ1BKRVpHZkt5aGRzVVVFR1VlZ0NMWkVmbWk0d2ZoV21tdWcxZ2dVOFdLbVZ5NTEzejUyY1h6YkpQM3NNYXNaZno0UkJ1YlZKdWlMUzJ1VG5EMkpObGV2cEwzdkJHR1Z2T2lCb1gyakhDbDJGNUdNV0wvZTdKaUtKOWtNenNENWpidktBNFJDQ0h6NVFmL0tnU0Z6ZU1qSHFQSnFCQmV4bk0wQ3dJWlBhaCtMYWcyb1RLZGttZlFpZnRzSjZwNnE3ZVI5ejRRMkl4U1JQbGhmRThLWkMxZExCd1NOMUgySFhkWDZ0SW1QSVBtNnd3eGd1eVJab0JzSC8xRTY3N0h1VWFKUEE0eFdOTkcwaXdOR044TnNLek5OMEdkMHN4OW56YkhsYlFEZEJLVFlIMUNDRk16TmNTdUJqaU1hOFQ3c3pTVStoeDFwRmplYWlYTFJ6d1pqdUlkeDQ1MnQ4b0RKY0VZNEI3KzZEQm9uTGs4TVlTNGZsUm5wcGZabnB3TERNcVVVZFRNNkJjRExGOXNhdlV1dzhaOG9OUm1jQjEwTlB3ZEplYzBzRWRoSDNNRThvVkFoWjZ2dHRFa2lLQmhPZXZjY2w4Umh1ekE1YnVjZFc2M0RkQVFPWDJGUzFoNG0vRGxIeU9PMFA3NG5WdTkzQUlnc0hPVGsvbFlxWnZXUVh5dmg4NHk4U1FQRm1wejBXOHduT1pIMUdXTXRXMkt2em8vU2phb2ZoU0NYa0FWa3hBN1dGU2llL2pCUlRxVzc0WWsxQTRIRHpCaml6UlZJM0FKRCtiWldjMGNFSlV0NHBOQ3pVQmY3M2crU0pKOWErUDVvbkI8L1g1MDlDZXJ0aWZpY2F0ZT4KICAgICAgPC9YNTA5RGF0YT4KICAgIDwvS2V5SW5mbz4KICA8L2RzOlNpZ25hdHVyZT4KICA8U3ViamVjdD4KICAgIDxOYW1lSUQ+c2FtcGxlLnRlc3QuYXp1cmUubmV0PC9OYW1lSUQ+CiAgICA8U3ViamVjdENvbmZpcm1hdGlvbiBNZXRob2Q9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpjbTpiZWFyZXIiIC8+CiAgPC9TdWJqZWN0PgogIDxDb25kaXRpb25zIE5vdEJlZm9yZT0iMjAxOS0wNi0xOFQwNzowODoxMS41MThaIiBOb3RPbk9yQWZ0ZXI9IjIwNzktMDYtMTlUMDc6MDg6MTEuNTE4WiI+CiAgICA8QXVkaWVuY2VSZXN0cmljdGlvbj4KICAgICAgPEF1ZGllbmNlPnN2YzovL0F6UHViU3ViU2VydmljZVRlc3RAc2FtcGxlLWRzdHMtYXdhcmUtc2VydmljZS5jb3JlLndpbmRvd3MtdHN0Lm5ldC88L0F1ZGllbmNlPgogICAgPC9BdWRpZW5jZVJlc3RyaWN0aW9uPgogIDwvQ29uZGl0aW9ucz4KICA8QXR0cmlidXRlU3RhdGVtZW50PgogICAgPEF0dHJpYnV0ZSBOYW1lPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIj4KICAgICAgPEF0dHJpYnV0ZVZhbHVlPmF6cHVic3ViY2xpZW50LXVzZWFzdC5jb3JlLndpbmRvd3MubmV0PC9BdHRyaWJ1dGVWYWx1ZT4KICAgIDwvQXR0cmlidXRlPgogICAgPEF0dHJpYnV0ZSBOYW1lPSJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL3dzLzIwMDgvMDYvaWRlbnRpdHkvY2xhaW1zL3JvbGUiPgogICAgICA8QXR0cmlidXRlVmFsdWU+VG9BdXRoZW50aWNhdGVBelB1YlN1YjwvQXR0cmlidXRlVmFsdWU+CiAgICA8L0F0dHJpYnV0ZT4KICA8L0F0dHJpYnV0ZVN0YXRlbWVudD4KPC9Bc3NlcnRpb24+\"" +
              "}"

            val principal = new KafkaPrincipal(KafkaPrincipal.TOKEN_TYPE, tokenJsonString)
            val localHost = java.net.InetAddress.getLocalHost
            val session = Session(principal, localHost)
            val resource = Resource(Topic, "testTopic")

            val authorizer: AzPubSubAclAuthorizer = EasyMock.partialMockBuilder(classOf[AzPubSubAclAuthorizer])
              .addMockedMethod("getAcls", classOf[Resource])
              .createMock()
            val acls = Set(Acl(new KafkaPrincipal("Role", "ToAuthenticateAzPubSub"), Allow, "*", All))
            EasyMock.expect(authorizer.getAcls(isA(classOf[Resource]))).andReturn(acls).anyTimes()
            suppress(MemberMatcher.methodsDeclaredIn(classOf[KafkaMetricsGroup]))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newGauge"))

            val logger: org.slf4j.Logger = EasyMock.mock(classOf[org.slf4j.Logger])

            Whitebox.setInternalState(authorizer, classOf[org.slf4j.Logger], logger.asInstanceOf[Any])
            EasyMock.expect(logger.info(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.debug(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.warn(isA(classOf[String]))).andVoid().anyTimes()

            val cache = new mutable.HashMap[String, Date]
            val validator = new mockPositiveTokenValidator

            Whitebox.setInternalState(authorizer, "cacheTokenLastValidatedTime", cache.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "tokenAuthenticator", validator.asInstanceOf[Any])

            EasyMock.replay(authorizer)

            assert(false == authorizer.authorize(session, All, resource))
            EasyMock.verify(authorizer)
      }
}
