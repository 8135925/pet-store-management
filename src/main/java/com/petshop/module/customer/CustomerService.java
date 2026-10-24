package com.petshop.module.customer;

import com.petshop.common.BizException;
import com.petshop.common.ErrorCode;
import com.petshop.common.Money;
import com.petshop.common.PageResult;
import com.petshop.module.reminder.PetEvent;
import com.petshop.module.reminder.PetEventMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final PetMapper petMapper;
    private final PetEventMapper petEventMapper;

    public CustomerService(CustomerMapper customerMapper, PetMapper petMapper, PetEventMapper petEventMapper) {
        this.customerMapper = customerMapper;
        this.petMapper = petMapper;
        this.petEventMapper = petEventMapper;
    }

    public PageResult<Customer> search(String keyword, boolean includeInactive, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 200);
        long off = (long) (p - 1) * s;
        if (off > Integer.MAX_VALUE) {
            off = Integer.MAX_VALUE;
        }
        String kw = keyword == null ? "" : keyword.trim();
        long total = customerMapper.countSearch(kw, includeInactive);
        List<Customer> rows = total == 0
                ? java.util.Collections.<Customer>emptyList()
                : customerMapper.search(kw, includeInactive, (int) off, s);
        return new PageResult<>(rows, total, p, s);
    }

    public Customer findById(Long id) {
        Customer c = customerMapper.findById(id);
        if (c == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "客户不存在");
        }
        return c;
    }

    public List<Pet> petsOf(Long ownerId) {
        return petMapper.findByOwner(ownerId);
    }

    public Pet petById(Long id) {
        return petMapper.findById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createCustomer(String name, String phone, String source, String remark) {
        name = trim(name, "客户姓名不能为空");
        phone = trim(phone, "手机号不能为空");
        if (customerMapper.findByPhone(phone) != null) {
            throw new BizException(ErrorCode.CUSTOMER_EXISTS);
        }
        Customer c = new Customer();
        c.setName(name);
        c.setPhone(phone);
        c.setSource(source);
        c.setRemark(remark);
        c.setCreatedAt(LocalDateTime.now());
        try {
            customerMapper.insert(c);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发建档时唯一索引兜底，转成可读提示
            throw new BizException(ErrorCode.CUSTOMER_EXISTS);
        }
        return c.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(Long id, String name, String phone, String source, String remark) {
        name = trim(name, "客户姓名不能为空");
        phone = trim(phone, "手机号不能为空");
        Customer exists = customerMapper.findByPhone(phone);
        if (exists != null && !exists.getId().equals(id)) {
            throw new BizException(ErrorCode.CUSTOMER_EXISTS);
        }
        Customer c = findById(id);
        c.setName(name);
        c.setPhone(phone);
        c.setSource(source);
        c.setRemark(remark);
        try {
            customerMapper.update(c);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BizException(ErrorCode.CUSTOMER_EXISTS);
        }
    }

    /** 有历史单据只允许停用，禁止物理删除 */
    @Transactional(rollbackFor = Exception.class)
    public void toggleCustomer(Long id, boolean active) {
        customerMapper.setActive(id, active);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long savePet(Long id, Long ownerId, String name, String species, String breed,
                        String gender, String birthday, String weightKg,
                        boolean neutered, String color, String habit) {
        name = trim(name, "宠物昵称不能为空");
        if (ownerId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "宠物必须关联主人");
        }
        Customer owner = customerMapper.findById(ownerId);
        if (owner == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "主人不存在");
        }
        Integer weightG = Money.toGram(weightKg);
        LocalDate birth = null;
        if (birthday != null && !birthday.trim().isEmpty()) {
            birth = LocalDate.parse(birthday.trim());
            if (birth.isAfter(LocalDate.now())) {
                throw new BizException(ErrorCode.PET_BIRTHDAY_INVALID);
            }
        }
        Pet p = id == null ? new Pet() : petMapper.findById(id);
        if (p == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "宠物不存在");
        }
        p.setOwnerId(ownerId);
        p.setName(name);
        p.setSpecies(species == null || species.isEmpty() ? "DOG" : species);
        p.setBreed(breed);
        p.setGender(gender == null || gender.isEmpty() ? "UNKNOWN" : gender);
        p.setBirthday(birth);
        p.setWeightG(weightG);
        p.setIsNeutered(neutered);
        p.setColor(color);
        p.setHabit(habit);
        if (id == null) {
            p.setCreatedAt(LocalDateTime.now());
            petMapper.insert(p);
        } else {
            petMapper.update(p);
        }
        return p.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void togglePet(Long id, boolean active) {
        petMapper.setActive(id, active);
    }

    /** 详情页聚合数据 */
    public List<PetEvent> remindersOfCustomer(Long customerId) {
        return petEventMapper.findPendingByCustomer(customerId);
    }

    private String trim(String s, String errMsg) {
        if (s == null || s.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, errMsg);
        }
        return s.trim();
    }
}
