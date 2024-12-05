package com.vanatta.helene.supplies.database.manage;

import static org.assertj.core.api.Assertions.assertThat;

import com.vanatta.helene.supplies.database.TestConfiguration;
import com.vanatta.helene.supplies.database.data.ItemStatus;
import com.vanatta.helene.supplies.database.supplies.site.details.SiteDetailDao;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ManageSiteDaoTest {

  static class Helper {
    static long getSiteId() {
      return TestConfiguration.getSiteId();
    }

    static long getSiteId(String siteName) {
      return TestConfiguration.getSiteId(siteName);
    }
  }

  @BeforeAll
  static void setUp() {
    TestConfiguration.setupDatabase();
  }

  @Test
  void fetchSites() {
    var results = ManageSiteDao.fetchSiteList(TestConfiguration.jdbiTest);
    assertThat(results).isNotEmpty();

    results.forEach(result -> assertThat(result.getId()).isNotEqualTo(0L));
    var names =
        results.stream()
            .map(ManageSiteController.SiteSelection::getName)
            .collect(Collectors.toList());
    assertThat(names).contains("site1");
    names.forEach(name -> assertThat(name).isNotNull());
  }

  /**
   * Update site5 to have different site field values. Validate that those fields change value. Use
   * site5 to not interfere with any other tests (no other tests use 'site5')
   */
  @Test
  void updateSite() {
    long siteId = Helper.getSiteId("site5");

    // confirm contact number is null before we update it.
    assertThat(SiteDetailDao.lookupSiteById(TestConfiguration.jdbiTest, siteId).getContactNumber())
        .isNull();

    ManageSiteDao.updateSiteField(
        TestConfiguration.jdbiTest, siteId, ManageSiteDao.SiteField.SITE_NAME, "new site name");
    ManageSiteDao.updateSiteField(
        TestConfiguration.jdbiTest, siteId, ManageSiteDao.SiteField.CONTACT_NUMBER, "999-596-111");
    ManageSiteDao.updateSiteField(
        TestConfiguration.jdbiTest, siteId, ManageSiteDao.SiteField.CITY, "new city");
    ManageSiteDao.updateSiteField(
        TestConfiguration.jdbiTest, siteId, ManageSiteDao.SiteField.COUNTY, "Buncombe");
    ManageSiteDao.updateSiteField(
        TestConfiguration.jdbiTest, siteId, ManageSiteDao.SiteField.STREET_ADDRESS, "new address");
    ManageSiteDao.updateSiteField(
        TestConfiguration.jdbiTest, siteId, ManageSiteDao.SiteField.WEBSITE, "new website");

    assertThat(SiteDetailDao.lookupSiteById(TestConfiguration.jdbiTest, siteId).getSiteName())
        .isEqualTo("new site name");
    assertThat(SiteDetailDao.lookupSiteById(TestConfiguration.jdbiTest, siteId).getContactNumber())
        .isEqualTo("999-596-111");
    assertThat(SiteDetailDao.lookupSiteById(TestConfiguration.jdbiTest, siteId).getCity())
        .isEqualTo("new city");
    assertThat(SiteDetailDao.lookupSiteById(TestConfiguration.jdbiTest, siteId).getCounty())
        .isEqualTo("Buncombe");
    assertThat(SiteDetailDao.lookupSiteById(TestConfiguration.jdbiTest, siteId).getAddress())
        .isEqualTo("new address");
    assertThat(SiteDetailDao.lookupSiteById(TestConfiguration.jdbiTest, siteId).getWebsite())
        .isEqualTo("new website");
  }

  @Test
  void validateSomeSiteFieldsCannotBeDeleted() {
    long siteId = Helper.getSiteId("site1");

    List.of(
            ManageSiteDao.SiteField.SITE_NAME,
            ManageSiteDao.SiteField.CITY,
            ManageSiteDao.SiteField.COUNTY,
            ManageSiteDao.SiteField.STREET_ADDRESS)
        .forEach(
            field ->
                org.junit.jupiter.api.Assertions.assertThrows(
                    ManageSiteDao.RequiredFieldException.class,
                    () ->
                        ManageSiteDao.updateSiteField(
                            TestConfiguration.jdbiTest, siteId, field, "")));
  }

  @Test
  void fetchSiteName() {
    long siteId = Helper.getSiteId();

    String result = ManageSiteDao.fetchSiteName(TestConfiguration.jdbiTest, siteId);

    assertThat(result).isEqualTo("site1");
  }

  @Nested
  class SiteStatus {

    @Test
    void siteStatusActive() {
      long siteId = Helper.getSiteId("site1");
      var result = ManageSiteDao.fetchSiteStatus(TestConfiguration.jdbiTest, siteId);
      assertThat(result.isActive()).isTrue();

      ManageSiteDao.updateSiteActiveFlag(TestConfiguration.jdbiTest, siteId, false);
      result = ManageSiteDao.fetchSiteStatus(TestConfiguration.jdbiTest, siteId);
      assertThat(result.isActive()).isFalse();

      ManageSiteDao.updateSiteActiveFlag(TestConfiguration.jdbiTest, siteId, true);
      result = ManageSiteDao.fetchSiteStatus(TestConfiguration.jdbiTest, siteId);
      assertThat(result.isActive()).isTrue();

      siteId = Helper.getSiteId("site2");
      result = ManageSiteDao.fetchSiteStatus(TestConfiguration.jdbiTest, siteId);
      assertThat(result.isActive()).isTrue();

      siteId = Helper.getSiteId("site3");
      result = ManageSiteDao.fetchSiteStatus(TestConfiguration.jdbiTest, siteId);
      assertThat(result.isActive()).isFalse();
    }

    @Test
    void setStatusAcceptingDonations() {
      long siteId = Helper.getSiteId("site1");
      var result = ManageSiteDao.fetchSiteStatus(TestConfiguration.jdbiTest, siteId);
      assertThat(result.isAcceptingDonations()).isTrue();

      ManageSiteDao.updateSiteAcceptingDonationsFlag(TestConfiguration.jdbiTest, siteId, false);
      result = ManageSiteDao.fetchSiteStatus(TestConfiguration.jdbiTest, siteId);
      assertThat(result.isAcceptingDonations()).isFalse();

      ManageSiteDao.updateSiteAcceptingDonationsFlag(TestConfiguration.jdbiTest, siteId, true);
      result = ManageSiteDao.fetchSiteStatus(TestConfiguration.jdbiTest, siteId);
      assertThat(result.isAcceptingDonations()).isTrue();

      siteId = Helper.getSiteId("site2");
      result = ManageSiteDao.fetchSiteStatus(TestConfiguration.jdbiTest, siteId);
      assertThat(result.isAcceptingDonations()).isFalse();

      siteId = Helper.getSiteId("site3");
      result = ManageSiteDao.fetchSiteStatus(TestConfiguration.jdbiTest, siteId);
      assertThat(result.isAcceptingDonations()).isTrue();
    }

    @Test
    void fetchSiteStatus_SiteType() {
      long siteId = Helper.getSiteId("site1");
      var result = ManageSiteDao.fetchSiteStatus(TestConfiguration.jdbiTest, siteId);
      assertThat(result.getSiteTypeEnum()).isNotNull();
    }

    @Test
    void lookupSiteDetailReturnsSiteType() {
      long siteId = Helper.getSiteId("site1");
      var details = SiteDetailDao.lookupSiteById(TestConfiguration.jdbiTest, siteId);
      assertThat(details.getSiteType()).isNotNull();
    }

    @Test
    void updateSiteType() {
      long siteId = Helper.getSiteId("site1");

      ManageSiteDao.updateSiteType(
          TestConfiguration.jdbiTest, siteId, ManageSiteDao.SiteType.DISTRIBUTION_SITE);

      var details = SiteDetailDao.lookupSiteById(TestConfiguration.jdbiTest, siteId);
      assertThat(details.getSiteType())
          .isEqualTo(ManageSiteDao.SiteType.DISTRIBUTION_SITE.siteTypeName);

      ManageSiteDao.updateSiteType(
          TestConfiguration.jdbiTest, siteId, ManageSiteDao.SiteType.SUPPLY_HUB);

      details = SiteDetailDao.lookupSiteById(TestConfiguration.jdbiTest, siteId);
      assertThat(details.getSiteType()).isEqualTo(ManageSiteDao.SiteType.SUPPLY_HUB.siteTypeName);
    }
  }

  @Test
  void fetchSiteInventory() {
    long siteId = Helper.getSiteId("site1");
    var result = ManageSiteDao.fetchSiteInventory(TestConfiguration.jdbiTest, siteId);

    ManageSiteDao.SiteInventory water = findItemByName(result, "water");
    assertThat(water.isActive()).isTrue();
    assertThat(water.getItemStatus()).isEqualTo(ItemStatus.AVAILABLE.getText());

    ManageSiteDao.SiteInventory clothes = findItemByName(result, "new clothes");
    assertThat(clothes.isActive()).isTrue();
    assertThat(clothes.getItemStatus()).isEqualTo(ItemStatus.URGENTLY_NEEDED.getText());

    ManageSiteDao.SiteInventory usedClothes = findItemByName(result, "used clothes");
    assertThat(usedClothes.isActive()).isTrue();
    assertThat(usedClothes.getItemStatus()).isEqualTo(ItemStatus.OVERSUPPLY.getText());

    ManageSiteDao.SiteInventory gloves = findItemByName(result, "gloves");
    assertThat(gloves.isActive()).isFalse();
    assertThat(gloves.getItemStatus()).isNull();

    ManageSiteDao.SiteInventory randomStuff = findItemByName(result, "random stuff");
    assertThat(randomStuff.isActive()).isFalse();
    assertThat(randomStuff.getItemStatus()).isNull();
  }

  private static ManageSiteDao.SiteInventory findItemByName(
      List<ManageSiteDao.SiteInventory> items, String itemName) {
    return items.stream()
        .filter(r -> r.getItemName().equalsIgnoreCase(itemName))
        .findAny()
        .orElseThrow();
  }
}
