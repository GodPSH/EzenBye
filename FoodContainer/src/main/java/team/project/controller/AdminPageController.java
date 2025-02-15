package team.project.controller;

import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import team.project.dao.OrderProductDAO;
import team.project.service.BannerService;
import team.project.service.MemberService;
import team.project.service.MessageService;
import team.project.service.NoMemberOrdersService;
import team.project.service.OrderProductService;
import team.project.service.OrdersService;
import team.project.service.ProductService;
import team.project.service.RecipeService;
import team.project.service.ServiceCenterService;
import team.project.util.PagingUtil;
import team.project.vo.BannerVO;
import team.project.vo.MemberVO;
import team.project.vo.MessageVO;
import team.project.vo.NoMemberOrdersVO;
import team.project.vo.OrderProductVO;
import team.project.vo.OrdersVO;
import team.project.vo.ProductVO;
import team.project.vo.RecipeVO;
import team.project.vo.SearchVO;
import team.project.vo.ServiceCenterVO;

@Controller
public class AdminPageController {

	@Autowired
	private MemberService memberService;
	@Autowired
	private ProductService productService;
	@Autowired
	private BannerService bannerService;
	@Autowired
	private OrdersService ordersService;
	@Autowired
	private OrderProductService orderProductService;
	@Autowired
	private NoMemberOrdersService noMemberOrdersService;
	@Autowired
	private MessageService messageService;
	@Autowired
	private ServiceCenterService service;
	@Autowired
	private RecipeService recipeService;
	@Autowired
	private OrderProductDAO orderProductDao;

	MemberVO member;
	
	// 관리자페이지 메인
	@RequestMapping(value = "admin.do", method = RequestMethod.GET)
	public String home(Model model, HttpServletRequest request)  throws Exception{
		
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우
				
				// 결산
				List<HashMap<String, String>> salesList = orderProductService.sales();
				model.addAttribute("salesList", salesList);
				
				// 수익
				List<ProductVO> revenueList = orderProductService.revenue();
				model.addAttribute("rList", revenueList);
				
				// 최근 문의 내역
				List<ServiceCenterVO> serviceList = service.adminService();
				model.addAttribute("sList", serviceList);
				
				// 등록중인 배너 3개 뿌리기
				List<BannerVO> bannerList = bannerService.adminMainBanner();
				model.addAttribute("bList", bannerList);
				
				// 최고매출 상품 3개 뿌리기
				List<ProductVO> productList = productService.adminMainProductList();
				model.addAttribute("pList", productList);
				
				return "adminPage/adminPage_main";
			}
		}
		
	}

	// 탈퇴 아닌 회원 조회페이지로 이동
	@RequestMapping(value = "member_list.do", method = RequestMethod.GET)
	public String memberList(Model model, HttpServletRequest request, SearchVO searchvo, int nowPage) throws Exception{
		
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우

				// del_yn = "N"
				searchvo.setDel_yn("N");
				// 현재페이지
				int realnowPage = 1;
				if(nowPage != 0) realnowPage = nowPage;
				
				// 리스트 출력
				List<MemberVO> memberList = memberService.adminMemberList(searchvo, realnowPage);
				model.addAttribute("memberList", memberList);
				
				// 검색값 + 페이징 출력
				PagingUtil paging = memberService.adminMemberPaging(searchvo, realnowPage);
				if(searchvo.getSearchValue() != null) {
					paging.setSearchValue(searchvo.getSearchValue());
					paging.setSearchType(searchvo.getSearchType());
				}
				model.addAttribute("paging", paging);
				
				return "adminPage/adminPage_member_list";
				
			}
		}
		
	}
	
	// 탈퇴 아닌 회원 상세 조회페이지로 이동
	@RequestMapping(value = "member_detail.do", method = RequestMethod.GET)
	public String memberDetail(Model model, HttpServletRequest request, SearchVO searchvo, int nowPage, int member_index) throws Exception{
		
		
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우
				MemberVO member = memberService.memberInfor(member_index);
				member.setPw("");
				model.addAttribute("m", member);
				
				// 검색&페이지
				model.addAttribute("searchType", searchvo.getSearchType());
				model.addAttribute("searchValue", searchvo.getSearchValue());
				model.addAttribute("nowPage", nowPage);
				
				return "adminPage/adminPage_member_detail";
			}
		}
		
	}
	
	// 회원을 추방하는 과정
	@RequestMapping(value = "memberDely.do", method = RequestMethod.POST)
	@ResponseBody
	public String memberDely(int member_index) throws Exception{
		int result = memberService.adminChangeMemberDel_yn(member_index);
		if(result > 0) {
			return "Success";
		}else {
			return "Fail";
		}
	}

	// 탈퇴된 회원 조회페이지로 이동
	@RequestMapping(value = "dely_list.do", method = RequestMethod.GET)
	public String memberDelyList(Model model, HttpServletRequest request, SearchVO searchvo, int nowPage) throws Exception{
		
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우

				// del_yn = "Y"
				searchvo.setDel_yn("Y");
				// 현재페이지
				int realnowPage = 1;
				if(nowPage != 0) realnowPage = nowPage;
				
				// 리스트 출력
				List<MemberVO> memberList = memberService.adminMemberList(searchvo, realnowPage);
				model.addAttribute("memberList", memberList);
				
				// 페이징 출력
				PagingUtil paging = memberService.adminMemberPaging(searchvo, realnowPage);
				model.addAttribute("paging", paging);
				
				return "adminPage/adminPage_delY_list";
				
				
			}
		}
		
	}
	
	// 회원에게 쪽지 보내기
	@RequestMapping(value = "sendMessage.do", method = RequestMethod.POST)
	@ResponseBody
	public String message(MessageVO vo) throws Exception{
		
		int result = messageService.messageInsert(vo);
		
		if(result == 1) {
			return "Success";
		}else {
			return "Fail";
		}
	}

	// 탈퇴한 회원 상세 조회페이지로 이동
	@RequestMapping(value = "dely_detail.do", method = RequestMethod.GET)
	public String memberDelYDetail(Model model, HttpServletRequest request, int nowPage, int member_index) throws Exception{
		
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우
				MemberVO member = memberService.memberInfor(member_index);
				member.setPw("");
				model.addAttribute("m", member);
				
				// 페이지
				model.addAttribute("nowPage", nowPage);
				
				return "adminPage/adminPage_delY_detail";
			}
		}
		
	}


	// 회원 주문 조회페이지로 이동
	@RequestMapping(value = "member_order_list.do", method = RequestMethod.GET)
	public String memberOrderList(Model model, HttpServletRequest request, SearchVO searchvo, int nowPage) throws Exception {
		
		
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우
				
				// 현재페이지
				int realnowPage = 1;
				if(nowPage != 0) realnowPage = nowPage;
				
				// 리스트 출력
				List<OrdersVO> memberOrdersList = ordersService.adminMemberOrdersList(searchvo, realnowPage);
				model.addAttribute("memberOrdersList", memberOrdersList);
		 
				// 검색값 + 페이징 출력
				PagingUtil paging = ordersService.adminMemberOrdersPaging(searchvo, realnowPage);
				if(searchvo.getSearchValue() != null) paging.setSearchValue(searchvo.getSearchValue());
				model.addAttribute("paging", paging);	
				
				return "adminPage/adminPage_member_order_list";
			}
		}
	}

	// 회원 주문 상세조회페이지로 이동
	@RequestMapping(value = "member_order_detail.do", method = RequestMethod.GET)
	public String memberOrderOne(Model model, HttpServletRequest request,
			OrdersVO ordersvo,
			@RequestParam(value="searchValue", required = false) String searchValue,
			@RequestParam(value="nowPage", required = false) int nowPage) throws Exception{
		
		
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우

				// 회원 주문 상세조회할때 주문들 불러오기
				List<OrderProductVO> adminMemberOrderProductList = orderProductService.adminMemberOrderProductList(ordersvo);
				model.addAttribute("opList", adminMemberOrderProductList);
				
				// 회원 주문 상세조회 할때 대략적인 정보
				OrdersVO order = ordersService.adminMemberOrder(ordersvo);
				model.addAttribute("order", order);
				
				// 회원 주문 상세조회 할때 쿠폰
				OrdersVO ordersDetailJoin = ordersService.ordersDetailJoin(ordersvo.getMember_order_index());
				model.addAttribute("orderCoupon",ordersDetailJoin);
				
				// 검색값&페이지
				model.addAttribute("searchValue", searchValue);
				model.addAttribute("nowPage", nowPage);
				
				return "adminPage/adminPage_member_order_detail";				
				
			}
		}
	}

	// 비회원 주문 조회페이지로 이동
	@RequestMapping(value = "noMember_order_list.do", method = RequestMethod.GET)
	public String noMemberOrderList(Model model, HttpServletRequest request, SearchVO searchvo, int nowPage) throws Exception{
		
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우
				
				// 현재페이지
				int realnowPage = 1;
				if(nowPage != 0) realnowPage = nowPage;
				
				// 리스트 출력
				List<NoMemberOrdersVO> noMemberOrdersList = noMemberOrdersService.adminNoMemberOrdersList(searchvo, realnowPage);
				model.addAttribute("noMemberOrdersList", noMemberOrdersList);
		 
				// 검색값 + 페이징 출력
				PagingUtil paging = noMemberOrdersService.adminNoMemberOrdersPaging(searchvo, realnowPage);
				if(searchvo.getSearchValue() != null) {
					paging.setSearchValue(searchvo.getSearchValue());
					paging.setSearchType(searchvo.getSearchType());
				}
				model.addAttribute("paging", paging);	
				
				return "adminPage/adminPage_noMember_order_list";				
				
			}
		}
	}

	// 비회원 주문 상세조회페이지로 이동
	@RequestMapping(value = "noMember_order_detail.do", method = RequestMethod.GET)
	public String noMemberOrderOne(Model model, HttpServletRequest request,
			@RequestParam(value="no_member_order_index") String no_member_order_index,
			@RequestParam(value="searchType", required = false) String searchType,
			@RequestParam(value="searchValue", required = false) String searchValue,
			@RequestParam(value="nowPage", required = false) int nowPage) throws Exception{
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우
				// 비회원 주문 상세 리스트 출력
				List<OrderProductVO> adminNoMemberOrderProductList = orderProductService.adminNoMemberOrderProductList(no_member_order_index);
				model.addAttribute("nopList", adminNoMemberOrderProductList);
				
				// 비회원 주문 대략적인 정보
				NoMemberOrdersVO order = noMemberOrdersService.noMemberOrdersList(no_member_order_index);
				model.addAttribute("order", order);
				
				// 검색값&페이지
				model.addAttribute("searchType", searchType);
				model.addAttribute("searchValue", searchValue);
				model.addAttribute("nowPage", nowPage);
				
				return "adminPage/adminPage_noMember_order_detail";			
			}
		}
		
	}
	
	// 배송완료처리 비동기
	@RequestMapping(value = "deliveryOk.do", method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.OK)
	public void deliveryOk(@RequestParam(value="orderItem_index") String orderItem_index) throws Exception{
		orderProductDao.deliveryOk(orderItem_index);
	}
	
	// 등록 상품 조회 페이지로 이동
	@RequestMapping(value = "product_main.do", method = RequestMethod.GET)
	public String productList(Model model, HttpServletRequest request, SearchVO searchvo, int nowPage) throws Exception {
		
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우
				
				// del_YN = "N"
				searchvo.setDel_YN("N");
				// 현재페이지
				int realnowPage = 1;
				if(nowPage != 0) realnowPage = nowPage;
				
				// 리스트 출력
				List<ProductVO> productList = productService.adminProductList(searchvo, realnowPage);
				model.addAttribute("productList", productList);
				
				// 검색값 + 페이징 출력
				PagingUtil paging = productService.adminProductPaging(searchvo, realnowPage);
				if(searchvo.getSearchValue() != null) paging.setSearchValue(searchvo.getSearchValue());
				model.addAttribute("paging", paging);
				
				// 검색값과 페이지를 세션에 저장하여 어느경로에서든 상풍 수정 페이지로 이동하고 다시 돌아왔을 때 입력값과 페이지가 온전하게 들어가게 하기 위한 세션
				session.setAttribute("nowPage", nowPage);
				if(searchvo.getSearchValue() != null) {
					session.setAttribute("searchValue", searchvo.getSearchValue());
				}else {
					session.setAttribute("searchValue", "");
				}
				session.setAttribute("route", 1);

				return "adminPage/adminPage_product_main";				
				
				
			}
		}

	}
	
	// 등록 상품 조회 페이지에서 선택 상품 삭제하는 비동기
	@RequestMapping(value = "productDelete.do", method = RequestMethod.POST)
	@ResponseBody
	public String productDelete(Model model, @RequestParam(value="product_index") String[] productIndexArr) throws Exception {
		
		int result = productService.adminProductDelYNisY(productIndexArr);
		
		if(result > 0) {
			return "deleteSuccess";
		}else {
			return "deleteFail";
		}
		
	}

	// 등록 상품 상세조회 페이지로 이동
	@RequestMapping(value = "product_detail.do", method = RequestMethod.GET)
	public String productOne(Model model, HttpServletRequest request,String product_index) throws Exception {
	
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우

				ProductVO product = productService.adminProductSelectOne(product_index);
				model.addAttribute("product", product);
				
				session.setAttribute("route", 2);
				
				return "adminPage/adminPage_product_detail";
			}
		}
	}

	// 상품 등록 페이지로 이동
	@RequestMapping(value = "product_register.do", method = RequestMethod.GET)
	public String productRegister(HttpServletRequest request) {

		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우
				return "adminPage/adminPage_product_register";
			}
		}

	}

	// 상품을 실제로 DB에 등록하는 과정
	@RequestMapping(value = "product_register.do", method = RequestMethod.POST)
	public String productInsert(ProductVO product,
								@RequestParam("tumnailImage") MultipartFile tumnailImage,
								@RequestParam("detailImage") MultipartFile detailImage,
								HttpServletRequest request) throws Exception {
		
		int result = productService.adminProductInsert(product, tumnailImage, detailImage, request);
		
		if(result == 1) {
			return "redirect:product_main.do?nowPage=1";
		}else {
			return "redirect:product_register.do";
		}
		
	}

	// 상품 수정 페이지로 이동
	@RequestMapping(value = "product_modify.do", method = RequestMethod.GET)
	public String productModify(Model model, HttpServletRequest request, String product_index) throws Exception {
		
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우

				ProductVO product = productService.adminProductSelectOne(product_index);
				model.addAttribute("product", product);
				
				return "adminPage/adminPage_product_modify";
			}
		}
	}
	
	// 상품을 실제로 DB에 수정하는 과정
	@RequestMapping(value = "product_modify.do", method = RequestMethod.POST)
	public String productUpdate(ProductVO product,
			 @RequestParam("tumnailImage") MultipartFile tumnailImage,
			 @RequestParam("detailImage") MultipartFile detailImage,
			 HttpServletRequest request) throws Exception {
		
		int result = productService.adminProductUpdate(product, tumnailImage, detailImage, request);
		
		if(result == 1) {
			return "redirect:product_main.do?nowPage=1";
		}else {
			return "redirect:product_modify.do?product_inex=" + product.getProduct_index();
		}
	}

	// 삭제된 상품 조회
	@RequestMapping(value = "product_delete_list.do", method = RequestMethod.GET)
	public String product_delete_list(Model model, HttpServletRequest request, SearchVO searchvo, int nowPage) throws Exception{
		
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우
				
				// del_YN = "Y"
				searchvo.setDel_YN("Y");
				// 현재페이지
				int realnowPage = 1;
				if(nowPage != 0) realnowPage = nowPage;
				
				// 리스트 출력
				List<ProductVO> productList = productService.adminProductList(searchvo, realnowPage);
				model.addAttribute("productList", productList);
				
				// 검색값 + 페이징 출력
				PagingUtil paging = productService.adminProductPaging(searchvo, realnowPage);
				if(searchvo.getSearchValue() != null) paging.setSearchValue(searchvo.getSearchValue());
				model.addAttribute("paging", paging);
				
				return "adminPage/adminPage_product_delete_list";		
			}
		}
		
	}
	
	// 삭제된 상품 상세조회
	@RequestMapping(value = "product_delete_detail.do", method = RequestMethod.GET)
	public String product_delete_detail(Model model, HttpServletRequest request,String product_index,
			SearchVO searchvo, int nowPage) throws Exception{
		
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우
				
				ProductVO product = productService.adminProductSelectOne(product_index);
				model.addAttribute("product", product);
				model.addAttribute("searchValue", searchvo.getSearchValue());
				model.addAttribute("nowPage", nowPage);
				
				return "adminPage/adminPage_product_delete_detail";				
			}
		}

	}

	// 상품 복구
	@RequestMapping(value = "changeDelYNinN.do", method = RequestMethod.POST)
	@ResponseBody
	public String productDelN(@RequestParam("product_index") String product_index) throws Exception{
		productService.adminProductDelYNisN(product_index);
		return "Success";
	}	

	// 배너 페이지 이동
	@RequestMapping(value = "banner.do", method = RequestMethod.GET)
	public String banner(Model model, HttpServletRequest request) throws Exception {
	
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우

				List<BannerVO> bannerList = bannerService.bannerList();
				model.addAttribute("bannerList", bannerList);
				
				return "adminPage/adminPage_banner";		
			}
		}
	}
	
	// 배너 등록
	@RequestMapping(value = "bannerRegister.do", method = RequestMethod.POST)
	public String bannerRegister(BannerVO bannervo,
								 @RequestParam("bannerFile") MultipartFile bannerFile,
								 HttpServletRequest request) throws Exception {
		
		int result = bannerService.bannerInsert(bannervo, bannerFile, request);
		
		if(result > 0) {
			return "redirect:banner.do";
		}else {
			return "redirect:admin.do";
		}
		
	}
	
	// 배너 수정
	@RequestMapping(value = "bannerModify.do", method = RequestMethod.POST)
	public String bannerModify(BannerVO bannervo,
							   @RequestParam("bannerFile") MultipartFile bannerFile,
							   HttpServletRequest request) throws Exception {
		
		int result = bannerService.bannerModify(bannervo, bannerFile, request);
		
		if(result > 0) {
			return "redirect:banner.do";
		}else {
			return "redirect:admin.do";
		}
	}
	
	// 배너 삭제
	@RequestMapping(value = "bannerDelete.do", method = RequestMethod.POST)
	@ResponseBody
	public String bannerDelete(@RequestParam(value="banner_index") int[] bannerIndexArr) throws Exception {
		
		int result = bannerService.bannerDelete(bannerIndexArr);
		
		if(result > 0) {
			return "deleteSuccess";
		}else {
			return "deleteFail";
		}
		
	}
	
	// 베스트 레시피
	@RequestMapping(value = "bestRecipe.do", method = RequestMethod.GET)
	public String bestRecipe(Model model, HttpServletRequest request) throws Exception{
		
		// 세션 소환
		HttpSession session = request.getSession();
		if(session.getAttribute("member") == null) {	// 로그인 안했을 때
			return "wrongAccessPage/needLogin";
		}else {
			member = (MemberVO)session.getAttribute("member");
			if(!member.getPosition().equals("관리자")) {	// 관리자가 아닐 경우
				return "wrongAccessPage/wrongAccess";
			}else {										// 관리자인 경우
				
				List<RecipeVO> bestRecipeList = recipeService.adminBestRecipeList();
				model.addAttribute("bestRecipeList", bestRecipeList);
				
				return "adminPage/adminPage_bestRecipe";
			}
		}
	}
	
	// 베스트 레시피 해제
	@RequestMapping(value = "bestRecipe.do", method = RequestMethod.POST)
	public String cancelRecipe(@RequestParam(value="recipe_index") int[] ridxArr) throws Exception{
		recipeService.adminCancelBestRecipe(ridxArr);
		return "redirect:bestRecipe.do";
	}
	
}
