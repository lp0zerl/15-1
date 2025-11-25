package ru.hogwarts.school;

import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management")
class ManagementController {
    private final BankRepository bankRepository;
    private final BuildProperties buildProperties;

    public ManagementController(BankRepository bankRepository, BuildProperties buildProperties) {
        this.bankRepository = bankRepository;
        this.buildProperties = buildProperties;
    }

    @PostMapping("/clear-caches")
    public ResponseEntity<Void> clearCaches() {
        bankRepository.clearAllCaches();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/info")
    public ResponseEntity<ServiceInfo> getServiceInfo() {
        ServiceInfo info = new ServiceInfo(
                buildProperties.getName(),
                buildProperties.getVersion()
        );
        return ResponseEntity.ok(info);
    }
}