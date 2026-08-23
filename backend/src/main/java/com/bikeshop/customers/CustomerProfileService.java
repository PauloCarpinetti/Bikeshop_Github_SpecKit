package com.bikeshop.customers;

import com.bikeshop.common.exception.NotFoundException;
import com.bikeshop.customers.dto.EnderecoDto;
import com.bikeshop.customers.dto.EnderecoRequest;
import com.bikeshop.customers.dto.ProfileDto;
import com.bikeshop.customers.dto.UpdateProfileRequest;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Atualização de perfil e gerenciamento de endereços do cliente autenticado (FR-008, T054).
 */
@Service
@Transactional
public class CustomerProfileService {

    private final ClienteRepository clienteRepository;
    private final EnderecoRepository enderecoRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerProfileService(ClienteRepository clienteRepository, EnderecoRepository enderecoRepository,
                                   PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.enderecoRepository = enderecoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public ProfileDto getProfile(Long clienteId) {
        return toProfileDto(requireCliente(clienteId));
    }

    public ProfileDto updateProfile(Long clienteId, UpdateProfileRequest request) {
        Cliente cliente = requireCliente(clienteId);
        cliente.updateNome(request.nome());
        cliente.updateTelefone(request.telefone());
        if (request.novaSenha() != null && !request.novaSenha().isBlank()) {
            cliente.updateSenhaHash(passwordEncoder.encode(request.novaSenha()));
        }
        return toProfileDto(cliente);
    }

    @Transactional(readOnly = true)
    public List<EnderecoDto> listAddresses(Long clienteId) {
        return enderecoRepository.findByClienteIdOrderByPadraoDescIdAsc(clienteId).stream()
                .map(this::toEnderecoDto)
                .toList();
    }

    public EnderecoDto createAddress(Long clienteId, EnderecoRequest request) {
        if (request.padrao()) {
            desmarcarPadraoAtual(clienteId);
        }
        Endereco endereco = new Endereco(clienteId, request.cep(), request.logradouro(), request.numero(),
                request.complemento(), request.bairro(), request.cidade(), request.estado(),
                resolveTipo(request.tipo()), request.padrao());
        return toEnderecoDto(enderecoRepository.save(endereco));
    }

    public EnderecoDto updateAddress(Long clienteId, Long enderecoId, EnderecoRequest request) {
        Endereco endereco = enderecoRepository.findByIdAndClienteId(enderecoId, clienteId)
                .orElseThrow(() -> new NotFoundException("Endereço", enderecoId));
        if (request.padrao()) {
            desmarcarPadraoAtual(clienteId);
        }
        endereco.atualizar(request.cep(), request.logradouro(), request.numero(), request.complemento(),
                request.bairro(), request.cidade(), request.estado(), resolveTipo(request.tipo()), request.padrao());
        return toEnderecoDto(endereco);
    }

    private void desmarcarPadraoAtual(Long clienteId) {
        enderecoRepository.findByClienteIdOrderByPadraoDescIdAsc(clienteId).stream()
                .filter(Endereco::isPadrao)
                .forEach(endereco -> endereco.marcarComoPadrao(false));
    }

    private EnderecoTipo resolveTipo(String tipo) {
        return tipo == null || tipo.isBlank() ? EnderecoTipo.ENTREGA : EnderecoTipo.valueOf(tipo);
    }

    private Cliente requireCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new NotFoundException("Cliente", clienteId));
    }

    private ProfileDto toProfileDto(Cliente cliente) {
        return new ProfileDto(cliente.getId(), cliente.getNome(), cliente.getEmail(), cliente.getTelefone());
    }

    private EnderecoDto toEnderecoDto(Endereco endereco) {
        return new EnderecoDto(endereco.getId(), endereco.getCep(), endereco.getLogradouro(), endereco.getNumero(),
                endereco.getComplemento(), endereco.getBairro(), endereco.getCidade(), endereco.getEstado(),
                endereco.getTipo().name(), endereco.isPadrao());
    }
}
