package br.edu.infnet.loanapp.core.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import br.edu.infnet.loanapp.business.model.Contract;
import br.edu.infnet.loanapp.business.model.Customer;
import br.edu.infnet.loanapp.business.model.Payment;
import br.edu.infnet.loanapp.business.repository.ContractRepository;
import br.edu.infnet.loanapp.business.repository.PaymentRepository;
import br.edu.infnet.loanapp.business.service.PaymentService;
import br.edu.infnet.loanapp.core.constants.URLConsts;
import br.edu.infnet.loanapp.core.dto.PaymentDTO;

@Controller
@RequestMapping(value = "/payment")
@SessionAttributes({ "clientSession" })
public class PaymentController implements BasicController {

	private final PaymentRepository paymentRepository;
	private final PaymentService paymentService;

	@Autowired
	private ContractRepository contractRepository;

	@Autowired
	public PaymentController(final PaymentRepository paymentRepository, final PaymentService paymentService) {
		this.paymentRepository = paymentRepository;
		this.paymentService = paymentService;
	}

	@PostMapping
	public ModelAndView contractForm(@ModelAttribute("paymentForm") final PaymentDTO paymentDto, final Model model) {

		ModelAndView modelAndView = new ModelAndView(URLConsts.getPaymentListPath());
		try {

			final Payment payment = Payment.fromDTO(paymentDto);

			this.paymentService.registerPayment(payment);
			model.addAttribute("message", "Pagamento realizado com sucesso!");
			modelAndView = new ModelAndView(URLConsts.getPaymentListPath());

			final Customer client = (Customer) model.getAttribute("clientSession");

			final Contract contract = payment.getInstallment().getContract();

			if (contract.getCustomer().getId() != client.getId()) {
				throw new RuntimeException("O contrato não é do cliente.");
			}

			final List<Payment> payments = this.paymentRepository.findAllPaymentByContractId(contract.getId());

			model.addAttribute("payments", payments);
			model.addAttribute("needMorePayment", payments//
					.stream()//
					.filter(Objects::nonNull)//
					.noneMatch(item -> item.getInstallment().getInstallmentNbr() == 1));
			model.addAttribute("contract", contract);
		} catch (final RuntimeException e) {
			model.addAttribute("message", e.getMessage());
		}
		return modelAndView;
	}

	@GetMapping
	public ModelAndView goToPayment(//
			@RequestParam(value = "id", required = true) final int id, //
			final Model model) {

		ModelAndView modelAndView = new ModelAndView(URLConsts.getPaymentListPath());
		try {

			modelAndView = new ModelAndView(URLConsts.getPaymentPath());

			final Customer client = (Customer) model.getAttribute("clientSession");

			final Contract contract = this.contractRepository.findById(id)
					.orElseThrow(() -> new RuntimeException("Nenhum contrato foi encontrado"));

			if (contract.getCustomer().getId() != client.getId()) {
				throw new RuntimeException("O contrato não é do cliente.");
			}

			final Payment payment = this.paymentService.getBasicPaymentBasedOnInstallment(contract);

			model.addAttribute("payment", payment);
		} catch (final RuntimeException e) {
			model.addAttribute("message", e.getMessage());
		}

		return modelAndView;
	}

	@Override
	public String getCurrentPath() {
		return URLConsts.getPaymentPath();
	}
}
