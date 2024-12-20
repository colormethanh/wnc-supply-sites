package com.vanatta.helene.supplies.database.manage.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.vanatta.helene.supplies.database.TestConfiguration;
import com.vanatta.helene.supplies.database.data.ItemStatus;
import com.vanatta.helene.supplies.database.manage.ManageSiteDao;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InventoryDaoTest {

  @BeforeAll
  static void setup() {
    TestConfiguration.setupDatabase();
  }

  @Test
  void updateSiteItemActive() {
    long siteId = TestConfiguration.getSiteId("site1");

    // first make sure 'gloves' are not active
    var result = ManageSiteDao.fetchSiteInventory(TestConfiguration.jdbiTest, siteId);
    ManageSiteDao.SiteInventory gloves = findItemByName(result, "gloves");
    assertThat(gloves.isActive()).isFalse();

    // set gloves to back to 'active'
    InventoryDao.updateSiteItemActive(TestConfiguration.jdbiTest, siteId, "gloves", "Oversupply");

    // verify gloves are active
    result = ManageSiteDao.fetchSiteInventory(TestConfiguration.jdbiTest, siteId);
    gloves = findItemByName(result, "gloves");
    assertThat(gloves.isActive()).isTrue();

    // set gloves to back to 'inactive'
    InventoryDao.updateSiteItemInactive(TestConfiguration.jdbiTest, siteId, "gloves");

    // verify gloves are inactive
    result = ManageSiteDao.fetchSiteInventory(TestConfiguration.jdbiTest, siteId);
    gloves = findItemByName(result, "gloves");
    assertThat(gloves.isActive()).isFalse();
  }

  @Test
  void updateSiteItemStatus() {
    long siteId = TestConfiguration.getSiteId("site1");

    // validate gloves status is 'Available'
    var result = ManageSiteDao.fetchSiteInventory(TestConfiguration.jdbiTest, siteId);
    ManageSiteDao.SiteInventory water = findItemByName(result, "water");
    assertThat(water.getItemStatus()).isEqualTo(ItemStatus.AVAILABLE.getText());

    // change gloves status to 'Urgent Need'
    InventoryDao.updateItemStatus(
        TestConfiguration.jdbiTest, siteId, "water", ItemStatus.URGENTLY_NEEDED.getText());

    // validation (1)
    var newStatus = InventoryDao.fetchItemStatus(TestConfiguration.jdbiTest, siteId, "water");
    assertThat(newStatus).isEqualTo(ItemStatus.URGENTLY_NEEDED);

    // validation (2) water status is updated 'Urgent Need'
    result = ManageSiteDao.fetchSiteInventory(TestConfiguration.jdbiTest, siteId);
    water = findItemByName(result, "water");
    assertThat(water.getItemStatus()).isEqualTo(ItemStatus.URGENTLY_NEEDED.getText());

    // change water status to 'Oversupply'
    InventoryDao.updateItemStatus(
        TestConfiguration.jdbiTest, siteId, "water", ItemStatus.OVERSUPPLY.getText());

    // validate water status is updated 'Oversupply'
    newStatus = InventoryDao.fetchItemStatus(TestConfiguration.jdbiTest, siteId, "water");
    assertThat(newStatus).isEqualTo(ItemStatus.OVERSUPPLY);

    result = ManageSiteDao.fetchSiteInventory(TestConfiguration.jdbiTest, siteId);
    water = findItemByName(result, "water");
    assertThat(water.getItemStatus()).isEqualTo(ItemStatus.OVERSUPPLY.getText());

    // change water status to 'Need'
    InventoryDao.updateItemStatus(
        TestConfiguration.jdbiTest, siteId, "water", ItemStatus.NEEDED.getText());

    // validate water status is updated 'Need'
    newStatus = InventoryDao.fetchItemStatus(TestConfiguration.jdbiTest, siteId, "water");
    assertThat(newStatus).isEqualTo(ItemStatus.NEEDED);

    result = ManageSiteDao.fetchSiteInventory(TestConfiguration.jdbiTest, siteId);
    water = findItemByName(result, "water");
    assertThat(water.getItemStatus()).isEqualTo(ItemStatus.NEEDED.getText());

    // change gloves status back to 'Available'
    InventoryDao.updateItemStatus(
        TestConfiguration.jdbiTest, siteId, "water", ItemStatus.AVAILABLE.getText());

    // validate gloves status is updated 'Need'
    result = ManageSiteDao.fetchSiteInventory(TestConfiguration.jdbiTest, siteId);
    water = findItemByName(result, "water");
    assertThat(water.getItemStatus()).isEqualTo(ItemStatus.AVAILABLE.getText());
  }

  @Test
  void addItem() {
    int itemCountPreInsert = countItems();

    boolean result = InventoryDao.addNewItem(TestConfiguration.jdbiTest, "new item");
    assertThat(result).isTrue();

    int itemCountPostInsert = countItems();
    assertThat(itemCountPreInsert + 1).isEqualTo(itemCountPostInsert);
  }

  private static int countItems() {
    return TestConfiguration.jdbiTest.withHandle(
        handle -> handle.createQuery("select count(*) from item").mapTo(Integer.class).one());
  }

  @Test
  void duplicateItemIsNoOp() {
    InventoryDao.addNewItem(TestConfiguration.jdbiTest, "some item");
    boolean result = InventoryDao.addNewItem(TestConfiguration.jdbiTest, "SOME ITEM");
    assertThat(result).isFalse();
  }

  private static ManageSiteDao.SiteInventory findItemByName(
      List<ManageSiteDao.SiteInventory> items, String itemName) {
    return items.stream()
        .filter(r -> r.getItemName().equalsIgnoreCase(itemName))
        .findAny()
        .orElseThrow();
  }

  @Test
  void validateSiteAuditChanges() {
    String name = UUID.randomUUID().toString();
    int startCount = countSiteItemAuditRecords();

    InventoryDao.addNewItem(TestConfiguration.jdbiTest, name);

    // adding a new item should not change the site_item_audit count
    assertThat(countSiteItemAuditRecords()).isEqualTo(startCount);

    long site1Id = TestConfiguration.getSiteId("site1");
    InventoryDao.updateSiteItemActive(
        TestConfiguration.jdbiTest, site1Id, name, ItemStatus.AVAILABLE.getText());
    assertThat(countSiteItemAuditRecords()).isEqualTo(startCount + 1);

    InventoryDao.updateItemStatus(
        TestConfiguration.jdbiTest, site1Id, name, ItemStatus.URGENTLY_NEEDED.getText());
    assertThat(countSiteItemAuditRecords()).isEqualTo(startCount + 2);
    InventoryDao.updateItemStatus(
        TestConfiguration.jdbiTest, site1Id, name, ItemStatus.NEEDED.getText());
    assertThat(countSiteItemAuditRecords()).isEqualTo(startCount + 3);

    InventoryDao.updateSiteItemInactive(TestConfiguration.jdbiTest, site1Id, name);
    assertThat(countSiteItemAuditRecords()).isEqualTo(startCount + 4);
  }

  private static int countSiteItemAuditRecords() {
    return TestConfiguration.jdbiTest.withHandle(
        handle ->
            handle.createQuery("select count(*) from site_item_audit").mapTo(Integer.class).one());
  }
}
