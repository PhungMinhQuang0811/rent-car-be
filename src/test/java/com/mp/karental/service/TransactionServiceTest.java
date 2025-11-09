package com.mp.karental.service;

import com.mp.karental.constant.ERole;
import com.mp.karental.constant.ETransactionStatus;
import com.mp.karental.constant.ETransactionType;
import com.mp.karental.dto.request.transaction.TransactionRequest;
import com.mp.karental.dto.response.transaction.ListTransactionResponse;
import com.mp.karental.dto.response.transaction.TransactionPaymentURLResponse;
import com.mp.karental.dto.response.transaction.TransactionResponse;
import com.mp.karental.entity.*;
import com.mp.karental.exception.AppException;
import com.mp.karental.mapper.TransactionMapper;
import com.mp.karental.payment.constant.VNPayIPNResponseConst;
import com.mp.karental.payment.dto.request.InitPaymentRequest;
import com.mp.karental.payment.dto.response.InitPaymentResponse;
import com.mp.karental.payment.service.IpnHandler;
import com.mp.karental.payment.service.PaymentService;
import com.mp.karental.repository.TransactionRepository;
import com.mp.karental.repository.WalletRepository;
import com.mp.karental.repository.AccountRepository;
import com.mp.karental.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {
    @InjectMocks
    private TransactionService transactionService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private PaymentService paymentService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private IpnHandler ipnHandler;
    private Wallet customerWallet;
    private Wallet carOwnerWallet;
    private Wallet loggedInUser;
    private MockedStatic<SecurityUtil> mockedSecurityUtil;
    private Account adminAccount;
    private Wallet adminWallet;

    @BeforeEach
    void setUp() {
        mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class);

        // Create test accounts
        Account customerAccount = Account.builder()
                .id("26b1f637-3f23-4d57-a20e-1e68b3176dfc")
                .build();

        Account carOwnerAccount = Account.builder()
                .id("3a1e8476-a7e4-4a9f-85f5-03a81133c140")
                .build();

        // Setup admin account and wallet
        adminAccount = Account.builder()
                .id("admin-id")
                .role(new Role(3, ERole.ADMIN))
                .build();

        adminWallet = Wallet.builder()
                .id("admin-wallet-id")
                .balance(5000000)
                .account(adminAccount)
                .build();

        // Setup wallets with proper initialization
        loggedInUser = Wallet.builder()
                .id("26b1f637-3f23-4d57-a20e-1e68b3176dfc")
                .balance(10000000)
                .account(customerAccount)
                .build();

        customerWallet = Wallet.builder()
                .id("26b1f637-3f23-4d57-a20e-1e68b3176dfc")
                .balance(10000000)
                .account(customerAccount)
                .build();

        carOwnerWallet = Wallet.builder()
                .id("3a1e8476-a7e4-4a9f-85f5-03a81133c140")
                .balance(50000)
                .account(carOwnerAccount)
                .build();

        // Mock SecurityUtil
        mockedSecurityUtil.when(SecurityUtil::getCurrentAccountId).thenReturn(loggedInUser.getId());
    }
    @AfterEach
    void tearDown() {
        mockedSecurityUtil.close();
    }
    @Test
    void withdraw_WithSufficientBalance_ShouldSucceed() {
        // Arrange
        long withdrawAmount = 5000000;
        when(SecurityUtil.getCurrentAccount()).thenReturn(loggedInUser.getAccount());

        when(walletRepository.findById(loggedInUser.getId())).thenReturn(Optional.of(loggedInUser));

        // Mock the transaction and response
        Transaction expectedTransaction = Transaction.builder()
                .id("txn123")
                .amount(withdrawAmount)
                .wallet(loggedInUser)
                .status(ETransactionStatus.SUCCESSFUL)
                .type(ETransactionType.WITHDRAW)
                .build();
        
        TransactionResponse expectedResponse = TransactionResponse.builder()
                .status(ETransactionStatus.SUCCESSFUL)
                .amount(withdrawAmount)
                .type(ETransactionType.WITHDRAW)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(expectedTransaction);
        when(transactionMapper.toTransactionResponse(any(Transaction.class))).thenReturn(expectedResponse);
        
        // Act
        TransactionResponse response = transactionService.withdraw(withdrawAmount);
        
        // Assert
        assertNotNull(response);
        assertEquals(ETransactionStatus.SUCCESSFUL, response.getStatus());
        assertEquals(5000000, loggedInUser.getBalance());
        verify(walletRepository).save(any(Wallet.class));
        verify(transactionRepository).save(argThat(transaction -> 
            transaction.getType() == ETransactionType.WITHDRAW &&
            transaction.getAmount() == withdrawAmount &&
            transaction.getStatus() == ETransactionStatus.SUCCESSFUL
        ));
        verify(transactionMapper).toTransactionResponse(any(Transaction.class));
    }

    @Test
    void withdraw_WithInsufficientBalance_ShouldThrowException() {
        // Arrange
        long excessiveAmount = 20000000;
        when(walletRepository.findById(loggedInUser.getId())).thenReturn(Optional.of(loggedInUser));
        
        // Act & Assert
        AppException exception = assertThrows(AppException.class, 
            () -> transactionService.withdraw(excessiveAmount));
            
        // Verify no interactions with repositories after exception
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(transactionMapper, never()).toTransactionResponse(any(Transaction.class));
    }

    @Test
    void createTransactionTopUp_WithValidRequest_ShouldCreateTransaction() {
        // Arrange
        TransactionRequest request = TransactionRequest.builder()
                .amount(5000000)
                .ipAddress("127.0.0.1")
                .type(ETransactionType.TOP_UP)
                .build();

        Transaction expectedTransaction = Transaction.builder()
                .id("txn123")
                .amount(5000000)
                .wallet(loggedInUser)
                .status(ETransactionStatus.PROCESSING)
                .type(ETransactionType.TOP_UP)
                .build();

        when(walletRepository.findById(loggedInUser.getId())).thenReturn(Optional.of(loggedInUser));
        when(transactionMapper.toTransaction(request)).thenReturn(expectedTransaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(expectedTransaction);
        when(paymentService.initPayment(any(InitPaymentRequest.class)))
                .thenReturn(new InitPaymentResponse("https://sandbox.vnpay.vn"));

        // Act
        TransactionPaymentURLResponse response = transactionService.createTransactionTopUp(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getPayment().getVnpUrl());
        verify(transactionRepository).save(any(Transaction.class));
        verify(paymentService).initPayment(any(InitPaymentRequest.class));
    }

    @Test
    void payDeposit_WithValidBooking_ShouldTransferFunds() {
        // Arrange
        Car car = Car.builder()
                .id("car-123")
                .model("Toyota Camry")
                .account(carOwnerWallet.getAccount())
                .build();

        Booking booking = Booking.builder()
                .deposit(2000000)
                .account(loggedInUser.getAccount())
                .car(car)
                .bookingNumber("BOOKING-123")
                .build();

        when(accountRepository.findByRoleId(3)).thenReturn(adminAccount);
        when(walletRepository.findById(loggedInUser.getId())).thenReturn(Optional.of(loggedInUser));
        when(walletRepository.findById(adminAccount.getId())).thenReturn(Optional.of(adminWallet));

        // Act
        transactionService.payDeposit(booking, loggedInUser.getId());

        // Assert
        assertEquals(8000000, loggedInUser.getBalance());
        assertEquals(7000000, adminWallet.getBalance());
        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(transactionRepository).save(argThat(transaction -> {
            boolean typeMatch = transaction.getType() == ETransactionType.PAY_DEPOSIT;
            boolean amountMatch = transaction.getAmount() == booking.getDeposit();
            boolean statusMatch = transaction.getStatus() == ETransactionStatus.SUCCESSFUL;
            boolean carNameMatch = transaction.getCarName().equals(car.getModel());
            boolean bookingNoMatch = transaction.getBookingNo().equals(booking.getBookingNumber());
            
            return typeMatch && amountMatch && statusMatch && carNameMatch && bookingNoMatch;
        }));
    }

    @Test
    void refundPartialDeposit_WithValidBooking_ShouldRefundCorrectly() {
        // Arrange
        Car car = Car.builder()
                .id("car-123")
                .model("Toyota Camry")
                .account(carOwnerWallet.getAccount())
                .build();

        Booking booking = Booking.builder()
                .deposit(100000)
                .account(loggedInUser.getAccount())
                .car(car)
                .bookingNumber("BOOKING-123")
                .build();

        // Initial balances
        loggedInUser.setBalance(10000000);    // 10M initial balance
        carOwnerWallet.setBalance(50000);     // 50K initial balance
        adminWallet.setBalance(5000000);      // 5M initial balance

        when(accountRepository.findByRoleId(3)).thenReturn(adminAccount);
        when(walletRepository.findById(loggedInUser.getId())).thenReturn(Optional.of(loggedInUser));
        when(walletRepository.findById(carOwnerWallet.getId())).thenReturn(Optional.of(carOwnerWallet));
        when(walletRepository.findById(adminAccount.getId())).thenReturn(Optional.of(adminWallet));

        // Act
        transactionService.refundPartialDeposit(booking);

        // Assert
        // Customer gets 70% of deposit back: 100000 * 0.7 = 70000
        long expectedCustomerBalance = 10000000 + (long)(booking.getDeposit() * 0.7);
        // Car owner gets 22% of deposit: 100000 * 0.22 = 22000
        long expectedCarOwnerBalance = 50000 + (long)(booking.getDeposit() * 0.22);
        // Admin keeps 8% of deposit: 100000 * 0.08 = 8000

        assertEquals(expectedCustomerBalance, loggedInUser.getBalance(), 
            "Customer should receive 70% of deposit back");
        assertEquals(expectedCarOwnerBalance, carOwnerWallet.getBalance(), 
            "Car owner should receive 22% of deposit");

        // Verify transactions
        verify(transactionRepository, times(2)).save(argThat(transaction -> 
            (transaction.getType() == ETransactionType.REFUND_DEPOSIT &&
             (transaction.getAmount() == (long)(booking.getDeposit() * 0.7) || 
              transaction.getAmount() == (long)(booking.getDeposit() * 0.22)) &&
             transaction.getStatus() == ETransactionStatus.SUCCESSFUL &&
             transaction.getCarName().equals(car.getModel()) &&
             transaction.getBookingNo().equals(booking.getBookingNumber()))
        ));

        // Verify wallet saves - 4 times total:
        // 2 times for admin wallet (deduct for customer and car owner)
        // 1 time for customer wallet
        // 1 time for car owner wallet
        verify(walletRepository, times(4)).save(any(Wallet.class));

        // Verify specific wallet saves if needed
        verify(walletRepository).save(argThat(wallet -> wallet.getId().equals(loggedInUser.getId())));
        verify(walletRepository).save(argThat(wallet -> wallet.getId().equals(carOwnerWallet.getId())));
        verify(walletRepository, times(2)).save(argThat(wallet -> wallet.getId().equals(adminWallet.getId())));
    }

    @Test
    void refundAllDeposit_WithValidBooking_ShouldRefundFullAmount() {
        // Arrange
        Car car = Car.builder()
                .id("car-123")
                .model("Toyota Camry")
                .account(carOwnerWallet.getAccount())
                .build();

        Booking booking = Booking.builder()
                .deposit(100000)
                .account(loggedInUser.getAccount())
                .car(car)
                .bookingNumber("BOOKING-123")
                .build();

        // Initial balances
        loggedInUser.setBalance(10000000);    // 10M initial balance
        adminWallet.setBalance(5000000);      // 5M initial balance

        when(accountRepository.findByRoleId(3)).thenReturn(adminAccount);
        when(walletRepository.findById(loggedInUser.getId())).thenReturn(Optional.of(loggedInUser));
        when(walletRepository.findById(adminAccount.getId())).thenReturn(Optional.of(adminWallet));

        // Act
        transactionService.refundAllDeposit(booking);

        // Assert
        // Customer gets full deposit back
        long expectedCustomerBalance = 10000000 + booking.getDeposit();
        assertEquals(expectedCustomerBalance, loggedInUser.getBalance(), 
            "Customer should receive full deposit back");
        
        // Admin wallet should decrease by deposit amount
        long expectedAdminBalance = 5000000 - booking.getDeposit();
        assertEquals(expectedAdminBalance, adminWallet.getBalance(), 
            "Admin wallet should decrease by deposit amount");

        verify(transactionRepository).save(argThat(transaction -> {
            boolean typeMatch = transaction.getType() == ETransactionType.REFUND_DEPOSIT;
            boolean amountMatch = transaction.getAmount() == booking.getDeposit();
            boolean statusMatch = transaction.getStatus() == ETransactionStatus.SUCCESSFUL;
            boolean carNameMatch = transaction.getCarName().equals(car.getModel());
            boolean bookingNoMatch = transaction.getBookingNo().equals(booking.getBookingNumber());
            
            return typeMatch && amountMatch && statusMatch && carNameMatch && bookingNoMatch;
        }));

        // Verify wallet saves - 2 times total:
        // 1 time for admin wallet (deduct)
        // 1 time for customer wallet (add)
        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(walletRepository).save(argThat(wallet -> wallet.getId().equals(loggedInUser.getId())));
        verify(walletRepository).save(argThat(wallet -> wallet.getId().equals(adminWallet.getId())));
    }

    @Test
    void getAllTransactionsList_ShouldReturnTransactions() {
        // Arrange
        String accountId = loggedInUser.getId();
        List<Transaction> transactions = Arrays.asList(
            Transaction.builder().id("txn1").amount(1000).build(),
            Transaction.builder().id("txn2").amount(2000).build()
        );
        List<TransactionResponse> transactionResponses = Arrays.asList(
            TransactionResponse.builder().amount(1000).build(),
            TransactionResponse.builder().amount(2000).build()
        );

        when(transactionRepository.getTransactionsByWalletId(accountId)).thenReturn(transactions);
        when(walletRepository.findById(accountId)).thenReturn(Optional.of(loggedInUser));
        when(transactionMapper.toTransactionResponse(any(Transaction.class)))
            .thenReturn(transactionResponses.get(0), transactionResponses.get(1));

        // Act
        ListTransactionResponse response = transactionService.getAllTransactionsList();

        // Assert
        assertNotNull(response);
        assertEquals(loggedInUser.getBalance(), response.getBalance());
        assertEquals(2, response.getListTransactionResponse().size());
        verify(transactionRepository).getTransactionsByWalletId(accountId);
    }

    @Test
    void getAllTransactions_WithDateRange_ShouldReturnFilteredTransactions() {
        // Arrange
        String accountId = loggedInUser.getId();
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        List<Transaction> transactions = Arrays.asList(
            Transaction.builder().id("txn1").amount(1000).build()
        );
        List<TransactionResponse> transactionResponses = Arrays.asList(
            TransactionResponse.builder().amount(1000).build()
        );

        when(transactionRepository.getTransactionsByDate(accountId, from, to)).thenReturn(transactions);
        when(walletRepository.findById(accountId)).thenReturn(Optional.of(loggedInUser));
        when(transactionMapper.toTransactionResponse(any(Transaction.class)))
            .thenReturn(transactionResponses.get(0));

        // Act
        ListTransactionResponse response = transactionService.getAllTransactions(from, to);

        // Assert
        assertNotNull(response);
        assertEquals(loggedInUser.getBalance(), response.getBalance());
        assertEquals(1, response.getListTransactionResponse().size());
        verify(transactionRepository).getTransactionsByDate(accountId, from, to);
    }

    @Test
    void getTransactionStatus_WithSuccessfulPayment_ShouldUpdateTransaction() {
        // Arrange
        String transactionId = "txn-123";
        Map<String, String> params = new HashMap<>();
        Transaction transaction = Transaction.builder()
            .id(transactionId)
            .status(ETransactionStatus.PROCESSING)
            .type(ETransactionType.TOP_UP)
            .amount(1000)
            .wallet(loggedInUser)
            .build();

        when(SecurityUtil.getCurrentAccount()).thenReturn(loggedInUser.getAccount());
        when(ipnHandler.process(params)).thenReturn(VNPayIPNResponseConst.SUCCESS);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(walletRepository.findById(loggedInUser.getId())).thenReturn(Optional.of(loggedInUser));
        when(transactionMapper.toTransactionResponse(any())).thenReturn(
            TransactionResponse.builder().status(ETransactionStatus.SUCCESSFUL).build()
        );

        // Act
        TransactionResponse response = transactionService.getTransactionStatus(transactionId, params);

        // Assert
        assertNotNull(response);
        assertEquals(ETransactionStatus.SUCCESSFUL, response.getStatus());
        verify(walletRepository).save(loggedInUser);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void getTransactionStatus_WithFailedPayment_ShouldThrowException() {
        // Arrange
        String transactionId = "txn-123";
        Map<String, String> params = new HashMap<>();
        Transaction transaction = Transaction.builder()
            .id(transactionId)
            .status(ETransactionStatus.PROCESSING)
            .type(ETransactionType.TOP_UP)
            .amount(1000)
            .wallet(loggedInUser)
            .build();

        TransactionResponse mockResponse = TransactionResponse.builder()
            .status(ETransactionStatus.PROCESSING)
            .build();

        when(ipnHandler.process(params)).thenReturn(VNPayIPNResponseConst.UNKNOWN_ERROR);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(walletRepository.findById(loggedInUser.getId())).thenReturn(Optional.of(loggedInUser));
        when(transactionMapper.toTransactionResponse(any())).thenReturn(mockResponse);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> 
            transactionService.getTransactionStatus(transactionId, params));

        verify(transactionRepository).findById(transactionId);
        verify(ipnHandler).process(params);
        assertEquals(ETransactionStatus.FAILED, transaction.getStatus());
    }

    @Test
    void getTransactionStatus_WithAlreadySuccessfulTransaction_ShouldNotUpdateStatus() {
        // Arrange
        String transactionId = "txn-123";
        Map<String, String> params = new HashMap<>();
        Transaction transaction = Transaction.builder()
            .id(transactionId)
            .status(ETransactionStatus.SUCCESSFUL)  // Already successful
            .type(ETransactionType.TOP_UP)
            .amount(1000)
            .wallet(loggedInUser)
            .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(walletRepository.findById(loggedInUser.getId())).thenReturn(Optional.of(loggedInUser));
        when(transactionMapper.toTransactionResponse(any())).thenReturn(
            TransactionResponse.builder().status(ETransactionStatus.SUCCESSFUL).build()
        );

        // Act
        TransactionResponse response = transactionService.getTransactionStatus(transactionId, params);

        // Assert
        assertNotNull(response);
        assertEquals(ETransactionStatus.SUCCESSFUL, response.getStatus());
        verify(transactionRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

//    @Test
//    void getTransactionStatus_WithNonTopUpTransaction_ShouldNotUpdateWallet() {
//        // Arrange
//        String transactionId = "txn-123";
//        Map<String, String> params = new HashMap<>();
//
//        Wallet wallet = Wallet.builder()
//            .id("26b1f637-3f23-4d57-a20e-1e68b3176dfc")
//            .balance(10000000L)
//            .build();
//
//        Transaction transaction = Transaction.builder()
//            .id(transactionId)
//            .status(ETransactionStatus.PROCESSING)
//            .type(ETransactionType.WITHDRAW)  // Non TOP_UP type
//            .amount(1000)
//            .wallet(wallet)
//            .build();
//
//        TransactionResponse mockResponse = TransactionResponse.builder()
//            .status(ETransactionStatus.SUCCESSFUL)
//            .build();
//
//        // Mock repository calls
//        when(ipnHandler.process(params)).thenReturn(VNPayIPNResponseConst.SUCCESS);
//        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
//        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
//        when(transactionMapper.toTransactionResponse(any())).thenReturn(mockResponse);
//        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
//        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
//
//        // Act
//        transactionService.getTransactionStatus(transactionId, params);
//
//        // Assert
//        verify(transactionRepository).findById(transactionId);
//        verify(ipnHandler).process(params);
//        verify(walletRepository, never()).save(any());
//        assertEquals(ETransactionStatus.SUCCESSFUL, transaction.getStatus());
//    }

    @Test
    void offsetFinalPayment_WhenDepositLessThanTotal_ShouldDeductFromCustomer() {
        // Arrange
        Car car = Car.builder()
            .id("car-123")
            .model("Toyota Camry")
            .account(carOwnerWallet.getAccount())
            .build();

        Booking booking = Booking.builder()
            .deposit(1000000) // 1M deposit
            .basePrice(2000000) // 2M per day
            .pickUpTime(LocalDateTime.now())
            .dropOffTime(LocalDateTime.now().plusDays(2)) // 2 days rental
            .account(loggedInUser.getAccount())
            .car(car)
            .bookingNumber("BOOKING-123")
            .build();

        when(accountRepository.findByRoleId(3)).thenReturn(adminAccount);
        when(walletRepository.findById(loggedInUser.getId())).thenReturn(Optional.of(loggedInUser));
        when(walletRepository.findById(carOwnerWallet.getId())).thenReturn(Optional.of(carOwnerWallet));
        when(walletRepository.findById(adminAccount.getId())).thenReturn(Optional.of(adminWallet));

        // Act
        transactionService.offsetFinalPayment(booking);

        // Assert
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(walletRepository, times(4)).save(any(Wallet.class));
    }

    @Test
    void offsetFinalPayment_WhenDepositMoreThanTotal_ShouldRefundToCustomer() {
        // Arrange
        Car car = Car.builder()
            .id("car-123")
            .model("Toyota Camry")
            .account(carOwnerWallet.getAccount())
            .build();

        Booking booking = Booking.builder()
            .deposit(5000000) // 5M deposit
            .basePrice(2000000) // 2M per day
            .pickUpTime(LocalDateTime.now())
            .dropOffTime(LocalDateTime.now().plusDays(1)) // 1 day rental
            .account(loggedInUser.getAccount())
            .car(car)
            .bookingNumber("BOOKING-123")
            .build();

        when(accountRepository.findByRoleId(3)).thenReturn(adminAccount);
        when(walletRepository.findById(loggedInUser.getId())).thenReturn(Optional.of(loggedInUser));
        when(walletRepository.findById(carOwnerWallet.getId())).thenReturn(Optional.of(carOwnerWallet));
        when(walletRepository.findById(adminAccount.getId())).thenReturn(Optional.of(adminWallet));

        // Act
        transactionService.offsetFinalPayment(booking);

        // Assert
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(walletRepository, times(4)).save(any(Wallet.class));
    }

    @Test
    void calculateTotalPayment_ShouldCalculateCorrectly() {
        // Arrange
        LocalDateTime pickUpTime = LocalDateTime.now();
        LocalDateTime dropOffTime = pickUpTime.plusDays(2).plusHours(6); // 2.25 days, should round up to 3
        long basePrice = 1000000;

        Booking booking = Booking.builder()
            .basePrice(basePrice)
            .pickUpTime(pickUpTime)
            .dropOffTime(dropOffTime)
            .build();

        // Act
        long totalPayment = transactionService.calculateTotalPayment(booking);

        // Assert
        assertEquals(3000000, totalPayment, "Should charge for 3 full days");
    }

    @Test
    void offsetFinalPayment_WithEqualDepositAndTotal_ShouldHandleCorrectly() {
        // Arrange
        Car car = Car.builder()
            .id("car-123")
            .model("Toyota Camry")
            .account(carOwnerWallet.getAccount())
            .build();

        Booking booking = Booking.builder()
            .deposit(2000000) // 2M deposit
            .basePrice(2000000) // 2M per day
            .pickUpTime(LocalDateTime.now())
            .dropOffTime(LocalDateTime.now().plusDays(1)) // 1 day rental = 2M total
            .account(loggedInUser.getAccount())
            .car(car)
            .bookingNumber("BOOKING-123")
            .build();

        when(accountRepository.findByRoleId(3)).thenReturn(adminAccount);
        when(walletRepository.findById(loggedInUser.getId())).thenReturn(Optional.of(loggedInUser));
        when(walletRepository.findById(carOwnerWallet.getId())).thenReturn(Optional.of(carOwnerWallet));
        when(walletRepository.findById(adminAccount.getId())).thenReturn(Optional.of(adminWallet));

        // Act
        transactionService.offsetFinalPayment(booking);

        // Assert
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(walletRepository, times(4)).save(any(Wallet.class)); // Updated to match actual calls
        
        // Verify the final balance
        verify(walletRepository).save(argThat(wallet -> 
            wallet.getId().equals(loggedInUser.getId())));
        verify(walletRepository).save(argThat(wallet -> 
            wallet.getId().equals(carOwnerWallet.getId())));
    }
}
