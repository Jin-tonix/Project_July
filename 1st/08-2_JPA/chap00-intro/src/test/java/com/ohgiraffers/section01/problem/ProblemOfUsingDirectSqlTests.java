package com.ohgiraffers.section01.problem;

import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class ProblemsOfUsingDirectSqlTests {

    private Connection con;

    @BeforeEach
    void setConnection() throws ClassNotFoundException, SQLException {
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://localhost:3306/gangnam";
        String user = "gangnam";
        String password = "gangnam";

        Class.forName(driver);
        con = DriverManager.getConnection(url, user, password);
        con.setAutoCommit(false);
    }

    @AfterEach
    void closeConnection() throws SQLException {
        con.rollback();
        con.close();
    }
    /*
     * JDBC API를 이용해 직접 SQL을 다룰 때 발생할 수 있는 문제점
     * 1. 데이터 변환, SQL 작성, JDBC API 코드 등의 중복 작성 (개발 시간 증가, 유지보수성 저하)
     * 2. SQL에 의존하여 개발
     * 3. 패러다임 불일치 (상속, 연관관계, 객체 그래프 탐색)
     * 4. 동일성 보장 문제
     * */


    // 1. 데이터 변환, sql 작성 , jdbc api 코드 등의 중복 작성 (개발 시간 증가, 유지보수성 저하)
    @DisplayName("직접 SQL을 작성하여 메뉴를 조회할 때 발생하는 문제")
    @Test
    void testDirectSelectSql() throws SQLException {
        // given
        String query = "SELECT MENU_CODE, MENU_NAME, MENU_PRICE, CATEGORY_CODE, ORDERABLE_STATUS FROM TBL_MENU";

        // when
        Statement stmt = con.createStatement();
        ResultSet rset = stmt.executeQuery(query);


        List<Menu> menuList = new ArrayList<>();
        while (rset.next()){
            Menu menu = new Menu();
            menu.setMenuCode(rset.getInt("MENU_CODE"));
            menu.setMenuName(rset.getString("MENU_NAME"));
            menu.setMenuPrice(rset.getInt("MENU_PRICE"));
            menu.setCategoryCode(rset.getInt("CATEGORY_CODE"));
            menu.setOrderableStatus(rset.getString("ORDERABLE_STATUS"));
            menuList.add(menu);
        }

        //then
        Assertions.assertNotNull(menuList);
        menuList.forEach(System.out::println);
        rset.close();
        stmt.close();
    }

    @DisplayName("직접 SQL을 작성하여 신규 메뉴를 추가할 때 발생하는 문제 확인")
    @Test
    void testDirectInsertSQL() throws SQLException {
        Menu menu = new Menu();
        menu.setMenuName("민트초코짜장면");
        menu.setMenuPrice(12000);
        menu.setCategoryCode(1);
        menu.setOrderableStatus("Y");

        String query = "INSERT INTO TBL_MENu(MENU_NAME, MENU_PRICE, CATEGORY_CODE, ORDERABLE_STATUS) VALUES(?,?,?,?)";

        PreparedStatement pstmt = con.prepareStatement(query);
        pstmt.setString(1, menu.getMenuName());
        pstmt.setInt(2, menu.getMenuPrice());
        pstmt.setInt(3, menu.getCategoryCode());
        pstmt.setString(4, menu.getOrderableStatus());

        int result = pstmt.executeUpdate();
        Assertions.assertEquals(1, result);
        pstmt.close();
    }

    /* 2. SQL에 의존하여 개발 */
    /* 요구사항의 변경에 따라 애플리케이션의 수정이 SQL의 수정으로도 이어진다.
     * 이러한 수정 영향을 미치는 것은 오류를 발생시킬 가능성도 있지만 유지보수성에도 악영향을 미친다.
     * 또한 객체를 사용할 때 SQL에 의존하면 객체에 값이 무엇이 들어있는지 확인하기 위해 SQL을 매번 살펴야 한다. */
    @DisplayName("조회 항목 변경에 따른 의존성 확인")
    @Test
    void testChangeSelectColums() throws SQLException {

        // given
        String query = "SELECT MENU_CODE, MENU_NAME FROM TBL_MENU";

        Statement stmt = con.createStatement();
        ResultSet rset = stmt.executeQuery(query);

        // when
        List<Menu> menuList = new ArrayList<>();
        while (rset.next()){
            Menu menu = new Menu();
            menu.setMenuCode(rset.getInt("MENU_CODE"));
            menu.setMenuName(rset.getString("MENU_NAME"));
//            menu.setMenuPrice(rset.getInt("MENU_PRICE"));
            menuList.add(menu);
        }

        // then
        Assertions.assertNotNull(menuList);
        menuList.forEach(System.out::println);
        rset.close();
        stmt.close();
    }

    /* 2-2. 연관된 객체 문제 */
    @DisplayName("연관된 객체 문제 확인")
    @Test
    void testAssociatedObject() throws SQLException {
        //given
        String query = "SELECT A.MENU_CODE, A.MENU_NAME, A.MENU_PRICE, B.CATEGORY_CODE, B.CATEGORY_NAME, A.ORDERABLE_STATUS "
                + "FROM TBL_MENU A "
                + "JOIN TBL_CATEGORY B ON (A.CATEGORY_CODE = B.CATEGORY_CODE)";

        //when
        Statement stmt = con.createStatement();
        ResultSet rset = stmt.executeQuery(query);

        List<MenuAndCategory> menuAndCategories = new ArrayList<>();
        while(rset.next()) {
            MenuAndCategory menuAndCategory = new MenuAndCategory();
            menuAndCategory.setMenuCode(rset.getInt("MENU_CODE"));
            menuAndCategory.setMenuName(rset.getString("MENU_NAME"));
            menuAndCategory.setMenuPrice(rset.getInt("MENU_PRICE"));
            menuAndCategory.setCategory(new Category(rset.getInt("CATEGORY_CODE"), rset.getString("CATEGORY_NAME")));
            menuAndCategory.setOrderableStatus(rset.getString("ORDERABLE_STATUS"));

            menuAndCategories.add(menuAndCategory);
        }

        //then
        Assertions.assertNotNull(menuAndCategories);
        menuAndCategories.forEach(System.out::println);

        rset.close();
        stmt.close();
    }

    /* 3. 패러다임 불일치 (상속, 연관관계, 객체 그래프 탐색) */
    /* 3-1. 상속 문제 */
    /* 객체 지향 언어의 상속 개념과 유사한 것이 데이터베이스의 서브타입엔티티이다.
     * 유사한 것 같지만 다른 부분은 데이터베이스의 상속은 상속 개념을 데이터로 추상화해서 슈퍼타입과 서브타입으로 구분하고,
     * 슈퍼타입의 모든 속성을 서브타입이 공유하지 못하여 물리적으로 다른 테이블로 분리가 된 형태이다.
     * (설계에 따라서는 속성으로 추가되기도 한다.)
     * 하지만 객체지향의 상속은 슈퍼타입의 속성을 공유해서 사용하므로 여기서 패러다임 불일치 현상이 발생한다. */

    /* 법인 회원과 일반 회원이라는 두 가지 회원을 추상화하여 회원이라는 슈퍼타입 엔터티를 도출하고
     * 서브타입 엔터티로 법인회원과 일반회원이 존재하는 상황인 경우
     * 물리적으로 회원과 법인회원 테이블로 분리된 상황에서는 각각 insert 구문을 수행해야 한다.
     * INSERT INTO 회원 ...
     * INSERT INTO 법인회원 ...
     * 하지만 JPA를 이용하여 상속을 구현한 경우에는
     * entityManager.persist(법인회원);
     * 이렇게 한 번에 해결이 가능하다.
     * */

    /* 3-2. 연관관계 문제, 객체 그래프 탐색 문제 */
    /* 객체지향에서 말하는 '가지고 있는(associtation 연관관계 혹은 collection 연관관계)'경우 데이터베이스 저장 구조와 다른 형태이다.
     * - 데이터베이스 테이블에 맞춘 객체 모델
     * public class Menu {
     *   private int menuCode;
     *   private String menuName;
     *   private int menuPrice;
     *   private int categoryCode;
     *   private String orderableStatus;
     * }
     * public class Category {
     *   private int categoryCode;
     *   private String categoryName;
     * }
     * - 객체 지향 언어에 맞춘 객체 모델 (JPA)
     * public class Menu {
     *   private int menuCode;
     *   private String menuName;
     *   private int menuPrice;
     *   private Category category;
     *   private String orderableStatus;
     * }
     * public class Category {
     *   private int categoryCode;
     *   private String categoryName;
     * }
     *
     * Menu menu = new Menu();
     * Category category = new Categroy();
     * menu.setCategory(category);   //메뉴와 카테고리의 관계 설정
     * menu.getCategory().getCategoryName(); //관계 설정되지 않으면 NPE 발생 가능성이 있음
     *
     * JPA는 간단하게 이 문제를 해결한다.
     * Menu menu = entityManager.find(Menu.class, menuCode);
     * menu.getCategory().getCategoryName(); //NPE가 발생하지 않음을 보장
     * */


    /* 4. 동일성 보장 문제 */
    @DisplayName("조회한 두 개의 행을 담은 객체의 동일성 비교 테스트")
    @Test
    void testEquals() throws SQLException {
        //given
        String query = "SELECT MENU_CODE, MENU_NAME FROM TBL_MENU WHERE MENU_CODE = 12";

        //when
        Statement stmt1 = con.createStatement();
        ResultSet rset1 = stmt1.executeQuery(query);

        Menu menu1 = null;

        while(rset1.next()) {
            menu1 = new Menu();
            menu1.setMenuCode(rset1.getInt("MENU_CODE"));
            menu1.setMenuName(rset1.getString("MENU_NAME"));
        }

        Statement stmt2 = con.createStatement();
        ResultSet rset2 = stmt2.executeQuery(query);

        Menu menu2 = null;

        while(rset2.next()) {
            menu2 = new Menu();
            menu2.setMenuCode(rset2.getInt("MENU_CODE"));
            menu2.setMenuName(rset2.getString("MENU_NAME"));
        }

        rset1.close();
        rset2.close();
        stmt1.close();
        stmt2.close();

        //then
//        Assertions.assertEquals(menu1, menu2);
        Assertions.assertFalse(menu1 == menu2);
    }

}
