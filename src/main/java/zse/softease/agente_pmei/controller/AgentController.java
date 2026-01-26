package zse.softease.agente_pmei.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import zse.softease.agente_pmei.dto.AgentLogDTO;
import zse.softease.agente_pmei.dto.AgentStateDTO;
import zse.softease.agente_pmei.service.AgentStateService;
import zse.softease.agente_pmei.service.LogService;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentStateService agentStateService;
    private final LogService logService;

    public AgentController(AgentStateService agentStateService,
                           LogService logService) {
        this.agentStateService = agentStateService;
        this.logService = logService;
    }

    @GetMapping("/status")
    public AgentStateDTO status() {
        return agentStateService.obterEstado();
    }

    @GetMapping("/logs")
    public List<AgentLogDTO> logs(
            @RequestParam(defaultValue = "50") int limite
    ) {
        return logService.ultimos(limite);
    }
}
