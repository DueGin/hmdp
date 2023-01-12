package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;


    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);

        // 2.判断秒杀是否开始
        if (LocalDateTime.now().isBefore(seckillVoucher.getBeginTime())) {
            return Result.fail("秒杀未开始！");
        }

        // 3.判断秒杀是否结束
        if (LocalDateTime.now().isAfter(seckillVoucher.getEndTime())) {
            return Result.fail("秒杀已结束");
        }

        // 4.判断库存是否充足
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足！");
        }

        // 5.一人一单
        Long userId = UserHolder.getUser().getId();
        // 1. 设置同步锁来实现一人一单，我们不希望将synchronized放在方法上，因为这样锁的是this，也就是整个对象
        // 2. 由于只需要用户自己锁自己，所以我们可以使用userId来锁住，当不同的用户来的时候，获得不同的锁，相同用户获取同一把锁
        // 3. 如果我们在进入方法后再上锁的话，那么在提交事务的时候，是先释放锁，再提交事务，这个就会导致问题。再释放锁后，其他线程就可以进入，那么
        //   新进来的线程他就会去查询，查询的仍是旧数据，这就也有可能导致一人多单了，所以需要先提交事务，再释放锁，应该锁住整个方法
        // 4. createVoucherOrder方法在这调用的话，this是当前对象，此时事务是不会生效的，这也是spring事务不生效的坑之一
        //    所以需要用代理对象来去调用才能生效，通过AopContext.currentProxy()可以获取当前对象的代理对象
        synchronized (userId.toString().intern()) {
            // 获取代理对象（事务）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(userId, voucherId);
        }
    }

    @Transactional
    @NotNull
    public Result createVoucherOrder(Long userId, Long voucherId) {
        // 5.1.查询订单
        long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.2.判断是否存在
        if (count > 0) {
            // 用户已经购买过了
            return Result.fail("您已经购买过一次了！");
        }

        // 6.扣减库存（实现乐观锁）
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > ? 乐观锁
                .update();

        if (!success) {
            return Result.fail("库存不足！");
        }

        // 6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 6.1.订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 6.2.用户id
        voucherOrder.setUserId(userId);
        // 6.3.代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        // 7.返回订单id
        return Result.ok(orderId);
    }
}
