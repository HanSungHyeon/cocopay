package com.cocopay.redis.service;

import com.cocopay.exception.dto.CustomException;
import com.cocopay.exception.dto.ErrorCode;
import com.cocopay.redis.key.BarcodeKey;
import com.cocopay.redis.mapper.RedisMapper;
import com.cocopay.redis.repository.BarcodeKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BarcodeKeyService {
    private final BarcodeKeyRepository barcodeKeyRepository;
    private final RedisMapper redisMapper;

    @Async
    public void barcodeSave(int userId, int cardId, String barcodeNum) {
        log.info("바코드 세이브 시작");
        long start = System.currentTimeMillis();

        BarcodeKey barcodeKey = redisMapper.toBarcodeKey(userId, cardId, barcodeNum);

        barcodeKeyRepository.save(barcodeKey);
        long end = System.currentTimeMillis();
        log.info("바코드 세이브 끝 : {}", end - start);
        log.info("####### 스레드 이름 : " + Thread.currentThread().getName());
    }

    public BarcodeKey findBarcode(int userId) {
        Optional<BarcodeKey> findBarcode = barcodeKeyRepository.findById(userId);

        return findBarcode
                .orElseThrow(() -> new CustomException(ErrorCode.DATA_NOT_FOUND));
    }

    public int findCardId(String barcodeNum) {
        return findByBarcode(barcodeNum).getCardId();
    }

    public BarcodeKey findByBarcode(String barcodeNum) {
        Optional<BarcodeKey> findBarcode = barcodeKeyRepository.findByBarcodeNum(barcodeNum);

        return findBarcode
                .orElseThrow(() -> new CustomException(ErrorCode.DATA_NOT_FOUND));
    }
}
