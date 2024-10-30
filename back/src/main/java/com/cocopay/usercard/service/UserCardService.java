package com.cocopay.usercard.service;

import com.cocopay.exception.dto.CustomException;
import com.cocopay.exception.dto.ErrorCode;
import com.cocopay.payment.dto.req.CardUuidListDto;
import com.cocopay.payment.dto.res.PerformanceResDto;
import com.cocopay.payment.dto.res.PerformanceResListDto;
import com.cocopay.payment.service.PaymentService;
import com.cocopay.redis.key.PerformanceKey;
import com.cocopay.redis.service.BarcodeKeyService;
import com.cocopay.redis.service.PerformanceKeyService;
import com.cocopay.user.entity.User;
import com.cocopay.user.repository.UserRepository;
import com.cocopay.usercard.dto.*;
import com.cocopay.usercard.entity.UserCard;
import com.cocopay.usercard.mapper.UserCardMapper;
import com.cocopay.usercard.repository.UserCardRepository;
import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCardService {
    private final UserCardRepository userCardRepository;
    private final UserRepository userRepository;
    private final BarcodeKeyService barcodeKeyService;
    private final PerformanceKeyService performanceKeyService;
    private final PaymentService paymentService;
    private final UserCardMapper userCardMapper;

    //카드 등록
    public Integer registUserCard(UserCardRegisterDto userCardRegisterDto, boolean cocopay) {
        WebClient webClient = WebClient.create();

        //api 주소
        String url = "http://localhost:8081/bank/card/search";

        //임시 동기 요청
        UserCardDto userCardDto = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userCardRegisterDto)
                .retrieve()
                .bodyToMono(UserCardDto.class)
                .block();

        //중복체크
        Optional<UserCard> check = userCardRepository.findByUserCardId(userCardDto.getUserCardId());
        if (check.isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_CARD);
        }
        User findUser = findUser(userCardRegisterDto.getUserId());

        System.out.println(userCardDto.getUserCardId());
        int count = userCardRepository.findUserCardListByCocoType(userCardRegisterDto.getUserId()).size() + 1;
        String encSerialNum = getEncSerialNum(userCardDto.getSerialNumber());
        log.info("encSerialNum : {}", encSerialNum);
        UserCard userCard = userCardMapper.toUserCard(findUser, encSerialNum, cocopay, count, userCardDto);

        return userCardRepository.save(userCard).getId();
    }

    //카드 번호 암호화
    public String getEncSerialNum(String serialNumber) {
        return serialNumber.substring(0, 7) + "**-****-" + serialNumber.substring(15);
    }

    //카드 번호 암호화 진행
    public List<UserCardDto> cardNumEncryption(List<UserCardDto> list) {
        for (UserCardDto userCardDto : list) {
            String serialNumber = userCardDto.getSerialNumber();

            String encSerialNum = getEncSerialNum(serialNumber);

            userCardDto.setSerialNumber(encSerialNum);
        }
        return list;

    }

    //카드 목록 조회(코코페이 포함)
    public List<UserCard> findAllUserCardList(Integer userId) {
        return userCardRepository.FindAllUserCard(userId);
    }

    //메인페이지 바코드 번호 세팅
    public List<MainCardDto> setBarcodeNum(List<MainCardDto> list, int userId) {
        Faker faker = new Faker(new Locale("ko"));
        long start = System.currentTimeMillis();

        for (MainCardDto mainCardDto : list) {
            String barcodeNum = faker.numerify("############");
            barcodeKeyService.barcodeSave(userId, mainCardDto.getId(), barcodeNum);
            mainCardDto.setBarcodeNum(barcodeNum);
        }

        long end = System.currentTimeMillis() - start;
        log.info("end : {}", end);

        return list.stream()
                .sorted(Comparator.comparing(MainCardDto::getCardOrder))
                .toList();
    }

    //카드 목록 조회(코코페이 빼고, 목록에 들어갈 카드 목록)
    public List<UserCard> findUserCardList(Integer userId) {
        return userCardRepository.findUserCardListByCocoType(userId);
    }

    //사용자 카드 삭제
    public void deleteUserCard(Integer cardId) {
        Optional<UserCard> userCard = userCardRepository.findById(cardId);
//        userCard.get().setWithdrawDate(LocalDateTime.now());
//        userCardRepository.save(userCard.get());
        userCardRepository.delete(userCard.get());
    }

    //사용자별 통계 - 전체
    public CategoryResponseDto getAllamount(FindHistoryByUserId findHistoryByUserId) {
        User user = userRepository.findById(findHistoryByUserId.getUserId()).get();
        findHistoryByUserId.setUserId(user.getUuid());
        WebClient webClient = WebClient.create();

        //api 주소
        String url = "http://localhost:8081/bank/card-history/total";

        //임시 동기 요청
        CategoryResponseDto categoryResponseDto = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(findHistoryByUserId)
                .retrieve()
                .bodyToMono(CategoryResponseDto.class)
                .block();

        return categoryResponseDto;
    }

    //사용자별 통계 - 소비
    public CategoryPriceResponseDto getAllPrice(FindHistoryByUserId findHistoryByUserId) {
        User user = userRepository.findById(findHistoryByUserId.getUserId()).get();
        findHistoryByUserId.setUserId(user.getUuid());

        WebClient webClient = WebClient.create();

        //api 주소
        String url = "http://localhost:8081/bank/card-history/total/price";

        //임시 동기 요청
        CategoryPriceResponseDto categoryPriceResponseDto = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(findHistoryByUserId)
                .retrieve()
                .bodyToMono(CategoryPriceResponseDto.class)
                .block();


        return categoryPriceResponseDto;
    }

    //사용자별 통계 - 혜택
    public CategoryDiscountResponseDto getAllDiscount(FindHistoryByUserId findHistoryByUserId) {
        User user = userRepository.findById(findHistoryByUserId.getUserId()).get();
        findHistoryByUserId.setUserId(user.getUuid());
        WebClient webClient = WebClient.create();

        //api 주소
        String url = "http://localhost:8081/bank/card-history/total/discount";

        //임시 동기 요청
        CategoryDiscountResponseDto categoryDiscountResponseDto = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(findHistoryByUserId)
                .retrieve()
                .bodyToMono(CategoryDiscountResponseDto.class)
                .block();


        return categoryDiscountResponseDto;
    }

    //카드 한달 이용내역
    public HistoryResDtoTemp getCardHistory(HistoryFindReqDto historyFindReqDto) {
        WebClient webClient = WebClient.create();

        //api 주소
        String url = "http://localhost:8081/bank/card-history";

        //임시 동기 요청
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(historyFindReqDto)
                .retrieve()
                .bodyToMono(HistoryResDtoTemp.class)
                .block();
    }

    public HistoryResDto calHistory(List<HistoryResponseDto> list) {
        long amountSum = list.stream()
                .mapToLong(HistoryResponseDto::getAmount)
                .sum();

        int discountSum = list.stream()
                .mapToInt(HistoryResponseDto::getDiscountAmount)
                .sum();

       return userCardMapper.toHistoryResDto(amountSum, discountSum, list);
    }

    //카드 정보 보내주기(카드 상세페이지 부분)
    public UserCardDetailResponseDto findUserCardDetail(Integer cardId) {

        UserCard userCard = userCardRepository.findById(cardId).get();
        WebClient webClient = WebClient.create();

        //api 주소
        String url = "http://localhost:8081/bank/performance/list";

        List<Integer> cardList = new ArrayList<>();
        cardList.add(userCard.getCardUuid());
        CardUuidListDto cardUuidListDto = new CardUuidListDto();
        cardUuidListDto.setCardUuidList(cardList);

        //임시 동기 요청
        PerformanceResListDto performanceResListDto = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cardUuidListDto)
                .retrieve()
                .bodyToMono(PerformanceResListDto.class)
                .block();

        PerformanceResDto performanceResDto = performanceResListDto.getPerformanceList().get(0);
        log.info("채워야하는 조건 금액 : " + String.valueOf(performanceResDto.getPrice()));
        log.info("이번달 총 금액 : " + String.valueOf(performanceResDto.getTotalPrice()));
        //남은 금액
        int price = 0;
        if (performanceResDto.getLevel() == performanceResDto.getNextLevel() || performanceResDto.getTotalPrice() >= performanceResDto.getPrice()) {
            price = 0;
        } else {
            price = performanceResDto.getPrice() - performanceResDto.getTotalPrice();
        }

        //퍼센트
        double percent;
        if (performanceResDto.getTotalPrice() >= performanceResDto.getPrice()) {
            percent = 100.0;
        } else {
            percent = ((double) performanceResDto.getTotalPrice() / performanceResDto.getPrice() * 100);
        }

        //카드 이미지
        String cardImge;
        if (userCard.getCardCustomImage() == null) {
            cardImge = userCard.getCardDefaultImage();
        } else {
            cardImge = userCard.getCardCustomImage();
        }

        UserCardDetailResponseDto userCardDetailResponseDto = UserCardDetailResponseDto.builder()
                .userCardId(cardId)
                .cardName(userCard.getCardName())
                .level(performanceResDto.getLevel())
                .nextLevel(performanceResDto.getNextLevel())
                .price(price)
                .percent(percent)
                .cardImage(cardImge)
                .build();
        return userCardDetailResponseDto;

    }

    //카드 우선순위 변경
    public void setCardOrder(List<Integer> cardUuidList) {
        int order = 1;
        for (Integer cardId : cardUuidList) {
            UserCard userCard = userCardRepository.findById(cardId).get();
            userCard.setCardOrder(order);
            userCardRepository.save(userCard);
            order++;
        }
    }

    public MainAmountDto getAmount(FindHistoryByUserId findHistoryByUserId) {
        WebClient webClient = WebClient.create();

        //api 주소
        String url = "http://localhost:8081/bank/card-history/total";

        //임시 동기 요청
        CategoryResponseDto categoryResponseDto = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(findHistoryByUserId)
                .retrieve()
                .bodyToMono(CategoryResponseDto.class)
                .block();
        MainAmountDto mainAmountDto = MainAmountDto.builder()
                .allPrice(categoryResponseDto.getAllPriceAmount())
                .allDiscountAmount(categoryResponseDto.getAllDiscountAmount())
                .build();

        return mainAmountDto;
    }

    public String makeBarcode(int userId, int cardId,Faker faker) {

        String barcodeNum = faker.numerify("############");
        barcodeKeyService.barcodeSave(userId, cardId, barcodeNum);

        return barcodeNum;
    }

    //카드 uuid리스트 추출
    public List<Integer> getCardUuidList(List<UserCardDto> list) {
        return list.stream()
                .map(UserCardDto::getUserCardId)
                .toList();
    }

    //redis에 있는 실적 정보와 매칭 진행
    public List<UserCardResDto> cardUuidEqPerformance(List<UserCardDto> list) {
        List<UserCardResDto> resDtoList = new ArrayList<>();

        for (UserCardDto userCardDto : list) {
            //redis에서 해당 실적 정보 조회
            PerformanceKey performanceKey = performanceKeyService.findPerformanceKey(userCardDto.getUserCardId());
            //그래피 비율 계산
            double graphRate = paymentService.getGraphRate(performanceKey.getTotalPrice(), performanceKey.getPrice());
            //소숫점 첫번째 자리까지만
            String format = String.format("%.1f", graphRate);

            resDtoList.add(userCardMapper.toUserCardResDto(userCardDto, format));
        }

        return resDtoList;
    }

    public List<Integer> getCardUuid(List<UserCard> list) {
        return list.stream()
                .map(UserCard::getCardUuid)
                .toList();
    }

    public List<CardListDto> getCardUuidEqPerformance(List<UserCard> list) {
        List<CardListDto> cardListDtoList = new ArrayList<>();
        for (UserCard userCard : list) {
            String cardImage = userCard.getCardCustomImage();
            PerformanceKey performanceKey = performanceKeyService.findPerformanceKey(userCard.getCardUuid());

            double graphRate = paymentService.getGraphRate(performanceKey.getTotalPrice(), performanceKey.getPrice());

            String format = String.format("%.1f", graphRate);

            if (cardImage == null) {
                cardImage = userCard.getCardDefaultImage();
            }
            cardListDtoList.add(userCardMapper.toCardListDto(userCard, format, cardImage));
        }
        return cardListDtoList;
    }

    public User findUser(int userId) {
        Optional<User> findUser = userRepository.findById(userId);

        return findUser
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public UserCard findUsersCard(int userCardId) {
        Optional<UserCard> findUserCard = userCardRepository.findById(userCardId);

        return findUserCard
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER_CARD));
    }
}
